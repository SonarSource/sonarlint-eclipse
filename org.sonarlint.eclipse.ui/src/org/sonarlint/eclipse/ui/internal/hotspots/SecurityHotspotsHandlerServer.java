/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.hotspots;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.MethodNotSupportedException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpFilterChain;
import org.apache.hc.core5.http.io.HttpFilterHandler;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.ProjectSelectionDialog;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.popup.FailedToOpenHotspotPopup;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.commons.TextRange;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspotDetails;
import org.sonarsource.sonarlint.shaded.com.google.gson.Gson;
import org.sonarsource.sonarlint.shaded.com.google.gson.annotations.SerializedName;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.defaultString;

public class SecurityHotspotsHandlerServer {

  static final int STARTING_PORT = 64120;
  static final int ENDING_PORT = 64130;

  private static final int INVALID_PORT = -1;

  @Nullable
  private HttpServer server;
  private int port;

  public void init() {
    final var socketConfig = SocketConfig.custom()
      .setSoTimeout(15, TimeUnit.SECONDS)
      .setTcpNoDelay(true)
      .build();
    port = INVALID_PORT;
    int triedPort = STARTING_PORT;
    HttpServer startedServer = null;
    while (port < 0 && triedPort <= ENDING_PORT) {
      try {
        startedServer = ServerBootstrap.bootstrap()
          .setLocalAddress(InetAddress.getLoopbackAddress())
          .setListenerPort(triedPort)
          .setSocketConfig(socketConfig)
          .addFilterFirst("CORS", new CorsFilter())
          .register("/sonarlint/api/status", new StatusRequestHandler())
          .register("/sonarlint/api/hotspots/show", new ShowHotspotRequestHandler())
          .create();
        startedServer.start();
        port = triedPort;
      } catch (Exception t) {
        SonarLintLogger.get().debug("Error while starting port: " + t.getMessage());
        triedPort++;
      }
    }
    if (port > 0) {
      SonarLintLogger.get().info("Started security hotspot handler on port " + port);
      server = startedServer;
    } else {
      SonarLintLogger.get().error("Unable to start security hotspot handler");
      server = null;
    }
  }

  public int getPort() {
    return port;
  }

  public boolean isStarted() {
    return server != null;
  }

  public void shutdown() {
    if (isStarted()) {
      server.close(CloseMode.IMMEDIATE);
      port = INVALID_PORT;
    }
  }

  private static class StatusRequestHandler implements HttpRequestHandler {

    @Override
    public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context) throws HttpException, IOException {
      var trustedServer = Optional.ofNullable(request.getHeader("Origin"))
        .map(Header::getValue)
        .map(SecurityHotspotsHandlerServer::isTrustedServer)
        .orElse(false);
      if (PlatformUI.isWorkbenchRunning()) {
        Display.getDefault().syncExec(() -> {
          var ideName = "Eclipse";
          var product = Platform.getProduct();
          if (product != null) {
            ideName = defaultString(product.getName(), "Eclipse");
          }
          var maybeWorkspaceInfo = trustedServer ? ResourcesPlugin.getWorkspace().getRoot().getLocation().lastSegment() : "";
          response.setEntity(new StringEntity(new StatusResponse(ideName, maybeWorkspaceInfo).toJson(), ContentType.APPLICATION_JSON));
          response.setCode(HttpStatus.SC_OK);
        });
      } else {
        response.setEntity(new StringEntity("Workbench is not running", ContentType.DEFAULT_TEXT));
        response.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }

  private static boolean isTrustedServer(String serverOrigin) {
    // A server is trusted if the Origin HTTP header matches one of the already configured servers
    // The Origin header has the following format: <scheme>://<host>(:<port>)
    // Since servers can have an optional "context path" after this, we consider a valid match when the server's configured URL begins with
    // the passed Origin
    // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin
    return SonarLintCorePlugin.getServersManager().getServers().stream().anyMatch(s -> s.getHost().startsWith(serverOrigin));
  }

  private static class ShowHotspotRequestHandler implements HttpRequestHandler {

    @Override
    public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
      throws HttpException, IOException {
      if (!Method.GET.isSame(request.getMethod())) {
        throw new MethodNotSupportedException("Only POST is supported");
      }
      var parameters = getParameters(request, response);
      if (parameters == null) {
        response.setCode(HttpStatus.SC_BAD_REQUEST);
        return;
      }

      response.setCode(HttpStatus.SC_OK);

      new Job("Opening security hotspot...") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          openSecurityHotspot(parameters);
          return Status.OK_STATUS;
        }
      }.schedule();
    }

    @Nullable
    private static ShowHotspotParameters getParameters(ClassicHttpRequest request, ClassicHttpResponse response)
      throws HttpException {
      var parameters = extractParameters(request);
      var projectKey = checkParameter(parameters, "project", response);
      if (projectKey == null) {
        return null;
      }
      var hotspotKey = checkParameter(parameters, "hotspot", response);
      if (hotspotKey == null) {
        return null;
      }
      var serverUrl = checkParameter(parameters, "server", response);
      if (serverUrl == null) {
        return null;
      }
      return new ShowHotspotParameters(projectKey, hotspotKey, serverUrl);
    }

    private static Map<String, String> extractParameters(ClassicHttpRequest request) throws HttpException {
      try {
        var parameters = URLEncodedUtils.parse(request.getUri().getQuery(), Charset.defaultCharset());
        return parameters.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
      } catch (URISyntaxException e) {
        throw new HttpException("Unable to parse the request URI", e);
      }
    }

    @Nullable
    private static String checkParameter(Map<String, String> parameters, String parameterName,
      ClassicHttpResponse response) {
      var parameterValue = parameters.get(parameterName);
      if (StringUtils.isEmpty(parameterValue)) {
        missingParameter(response, parameterName);
        return null;
      }
      return parameterValue;
    }

    private static void missingParameter(ClassicHttpResponse response, String parameterName) {
      response
        .setEntity(new StringEntity("Missing or empty '" + parameterName + "' parameter", ContentType.DEFAULT_TEXT));
      response.setCode(HttpStatus.SC_BAD_REQUEST);
    }

    private static void openSecurityHotspot(ShowHotspotParameters parameters) {
      SonarLintCorePlugin.getTelemetry().showHotspotRequestReceived();
      bringToFront();
      var serverConnections = SonarLintCorePlugin.getServersManager().findByUrl(parameters.serverUrl);
      if (serverConnections.isEmpty()) {
        var connection = createConnection(parameters.serverUrl);
        if (connection == null) {
          showErrorPopup("No connections found for URL: " + parameters.serverUrl);
          return;
        }
        serverConnections = List.of(connection);
      }
      var fetchedHotspotOpt = fetchHotspotFromAny(serverConnections, parameters);
      if (fetchedHotspotOpt.isEmpty()) {
        showErrorPopup("Unable to fetch hotspot details using configured connections");
        return;
      }
      var fetchedHotspot = fetchedHotspotOpt.get();
      var hotspotFile = findHotspotFile(fetchedHotspot, parameters.projectKey);
      if (hotspotFile.isEmpty()) {
        var boundProject = bindProjectTo(fetchedHotspot, parameters.projectKey);
        if (boundProject.isEmpty()) {
          showErrorPopup("No Eclipse projects bound to project '" + parameters.projectKey + "'");
          return;
        }
        hotspotFile = findHotspotFile(fetchedHotspot, boundProject.get());
        if (hotspotFile.isEmpty()) {
          showErrorPopup("Unable to find file '" + fetchedHotspot.hotspot.filePath + "' in bound project(s)");
          return;
        }
      }
      show(hotspotFile.get(), fetchedHotspot.hotspot);
    }

    private static void showErrorPopup(String msg) {
      Display.getDefault().asyncExec(() -> {
        var popup = new FailedToOpenHotspotPopup(msg);
        popup.open();
      });
    }

    private static void bringToFront() {
      Display.getDefault().syncExec(() -> {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        var shell = window.getShell();
        if (shell != null) {
          if (shell.getMinimized()) {
            shell.setMinimized(false);
          }
          shell.forceActive();
        }
      });
    }

    @Nullable
    private static IConnectedEngineFacade createConnection(String serverUrl) {
      var model = new ServerConnectionModel();
      model.setConnectionType(ConnectionType.ONPREMISE);
      model.setServerUrl(serverUrl);
      var wizard = new ServerConnectionWizard(model);
      wizard.setSkipBindingWizard(true);
      return DisplayUtils.syncExec(() -> {
        var dialog = ServerConnectionWizard.createDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
        dialog.setBlockOnOpen(true);
        dialog.open();
        return wizard.getResultServer();
      });
    }

    private static Optional<ISonarLintProject> bindProjectTo(FetchedHotspot fetchedHotspot, String projectKey) {
      var connection = fetchedHotspot.origin;
      var pickedProject = DisplayUtils.syncExec(
        () -> ProjectSelectionDialog.pickProject(fetchedHotspot.hotspot.filePath, projectKey, connection.getId()));
      if (pickedProject.isEmpty()) {
        return Optional.empty();
      }
      var project = pickedProject.get();
      var bindingJob = ProjectBindingProcess.scheduleProjectBinding(connection.getId(), List.of(project), projectKey);
      try {
        bindingJob.join();
      } catch (InterruptedException e) {
        SonarLintLogger.get().error("Cannot bind project", e);
        return Optional.empty();
      }
      return pickedProject;
    }

    private static Optional<ISonarLintFile> findHotspotFile(FetchedHotspot fetchedHotspot, String projectKey) {
      var projects = fetchedHotspot.origin.getBoundProjects(projectKey);
      for (var project : projects) {
        var hotspotFile = findHotspotFile(fetchedHotspot, project);
        if (hotspotFile.isPresent()) {
          return hotspotFile;
        }
      }
      return Optional.empty();
    }

    private static Optional<ISonarLintFile> findHotspotFile(FetchedHotspot fetchedHotspot, ISonarLintProject project) {
      return SonarLintCorePlugin.getServersManager().resolveBinding(project)
        .flatMap(binding -> binding.getProjectBinding().serverPathToIdePath(fetchedHotspot.hotspot.filePath))
        .flatMap(project::find);
    }

    private static class FetchedHotspot {
      public final IConnectedEngineFacade origin;
      public final ServerHotspotDetails hotspot;

      public FetchedHotspot(IConnectedEngineFacade origin, ServerHotspotDetails hotspot) {
        this.origin = origin;
        this.hotspot = hotspot;
      }
    }

    private static Optional<FetchedHotspot> fetchHotspotFromAny(List<IConnectedEngineFacade> serverConnections, ShowHotspotParameters parameters) {
      for (var serverConnection : serverConnections) {
        if (serverConnection != null) {
          var serverHotspot = serverConnection.getServerHotspot(parameters.hotspotKey, parameters.projectKey);
          if (serverHotspot.isPresent()) {
            return Optional.of(new FetchedHotspot(serverConnection, serverHotspot.get()));
          } else {
            SonarLintLogger.get().info("Cannot fetch security hotspot from connection '" + serverConnection.getId() + "'");
          }
        }
      }
      return Optional.empty();
    }

    private static void show(ISonarLintFile file, ServerHotspotDetails hotspot) {
      Display.getDefault().syncExec(() -> {
        var doc = getDocumentFromEditorOrFile(file);
        var marker = createMarker(file, hotspot, doc);
        try {
          var view = (HotspotsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(HotspotsView.ID);
          view.openHotspot(hotspot, marker);
        } catch (Exception e) {
          SonarLintLogger.get().error("Unable to open Hotspots View", e);
        }
      });
    }

    @Nullable
    private static IMarker createMarker(ISonarLintFile file, ServerHotspotDetails hotspot, IDocument doc) {
      IMarker marker = null;
      try {
        marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_HOTSPOT_ID);
        marker.setAttribute(IMarker.MESSAGE, hotspot.message);
        marker.setAttribute(IMarker.LINE_NUMBER, hotspot.textRange.getStartLine());
        var position = MarkerUtils.getPosition(doc,
          new TextRange(hotspot.textRange.getStartLine(), hotspot.textRange.getStartLineOffset(), hotspot.textRange.getEndLine(),
            hotspot.textRange.getEndLineOffset()));
        if (position != null && Objects.equals(hotspot.codeSnippet, doc.get(position.getOffset(), position.getLength()))) {
          marker.setAttribute(IMarker.CHAR_START, position.getOffset());
          marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
        }
      } catch (Exception e) {
        SonarLintLogger.get().debug("Unable to create hotspot marker", e);
      }
      return marker;
    }

    private static IDocument getDocumentFromEditorOrFile(ISonarLintFile file) {
      IDocument doc;
      var editorPart = PlatformUtils.findEditor(file);
      if (editorPart instanceof ITextEditor) {
        doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
      } else {
        doc = file.getDocument();
      }
      return doc;
    }

    private static class ShowHotspotParameters {
      public final String projectKey;
      public final String hotspotKey;
      public final String serverUrl;

      public ShowHotspotParameters(String projectKey, String hotspotKey, String serverUrl) {
        this.projectKey = projectKey;
        this.hotspotKey = hotspotKey;
        this.serverUrl = serverUrl;
      }
    }
  }

  private static class StatusResponse {
    @SerializedName("ideName")
    private final String ideName;
    @SerializedName("description")
    private final String description;

    public StatusResponse(String ideName, String description) {
      this.ideName = ideName;
      this.description = description;
    }

    public String toJson() {
      return new Gson().toJson(this);
    }
  }

  private static class CorsFilter implements HttpFilterHandler {

    @Override
    public void handle(ClassicHttpRequest request, HttpFilterChain.ResponseTrigger responseTrigger, HttpContext context, HttpFilterChain chain)
      throws HttpException, IOException {
      var origin = request.getHeader("Origin");
      chain.proceed(request, new HttpFilterChain.ResponseTrigger() {
        @Override
        public void sendInformation(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
          responseTrigger.sendInformation(classicHttpResponse);
        }

        @Override
        public void submitResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
          if (origin != null) {
            classicHttpResponse.addHeader("Access-Control-Allow-Origin", origin.getValue());
          }
          responseTrigger.submitResponse(classicHttpResponse);
        }
      }, context);
    }
  }

}
