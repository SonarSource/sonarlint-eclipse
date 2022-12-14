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
package org.sonarlint.eclipse.core.internal.backend;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.SonarLintClient;
import org.sonarsource.sonarlint.core.clientapi.client.fs.FindFileByNamesInScopeParams;
import org.sonarsource.sonarlint.core.clientapi.client.fs.FindFileByNamesInScopeResponse;
import org.sonarsource.sonarlint.core.clientapi.client.fs.FoundFileDto;
import org.sonarsource.sonarlint.core.commons.http.HttpClient;

/**
 * Headless part of the client
 *
 */
public abstract class SonarLintEclipseHeadlessClient implements SonarLintClient {

  @Override
  public CompletableFuture<FindFileByNamesInScopeResponse> findFileByNamesInScope(FindFileByNamesInScopeParams params) {
    var projectOpt = resolveProject(params.getConfigScopeId());
    if (projectOpt.isEmpty()) {
      return CompletableFuture.failedFuture(new IllegalStateException("Unable to resolve config scope: " + params.getConfigScopeId()));
    }
    List<FoundFileDto> results = new ArrayList<>();
    var resource = projectOpt.get().getResource();
    if (resource instanceof IContainer) {
      params.getFilenames().forEach(filename -> {
        var found = ((IContainer) resource).findMember(filename);
        if (found instanceof IFile) {
          var fsPath = found.getRawLocation() != null ? found.getRawLocation().toOSString() : found.getFullPath().toOSString();
          var iFile = (IFile) found;
          try {
            var content = new String(iFile.getContents().readAllBytes(), iFile.getCharset());
            results.add(new FoundFileDto(filename, fsPath, content));
          } catch (IOException | CoreException e) {
            SonarLintLogger.get().debug("Unable to read content of file: " + fsPath);
          }
        }
      });
    }
    return CompletableFuture.completedFuture(new FindFileByNamesInScopeResponse(results));
  }

  protected Optional<ISonarLintProject> resolveProject(String configScopeId) {
    var projectUri = URI.create(configScopeId);
    var projectOpt = Stream.of(ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(projectUri))
      .map(c -> Adapters.adapt(c, ISonarLintProject.class))
      .filter(Objects::nonNull)
      .findFirst();
    if (projectOpt.isEmpty()) {
      SonarLintLogger.get().debug("Unable to resolve config scope: " + configScopeId);
    }
    return projectOpt;
  }

  @Nullable
  @Override
  public HttpClient getHttpClient(String connectionId) {
    var connectionOpt = SonarLintCorePlugin.getServersManager().findById(connectionId);
    if (connectionOpt.isEmpty()) {
      return null;
    }
    return ((ConnectedEngineFacade) connectionOpt.get()).buildClientWithProxyAndCredentials();
  }

}
