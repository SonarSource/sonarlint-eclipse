/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.backend.SonarLintRpcClientSupportSynchronizer;
import org.sonarlint.eclipse.core.internal.http.EclipseUpdateSite;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.ui.internal.backend.SonarLintEclipseRpcClient;
import org.sonarlint.eclipse.ui.internal.console.SonarLintConsole;
import org.sonarlint.eclipse.ui.internal.extension.SonarLintUiExtensionTracker;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintFlowLocationsService;
import org.sonarlint.eclipse.ui.internal.popup.GenericNotificationPopup;
import org.sonarlint.eclipse.ui.internal.popup.NewerVersionAvailablePopup;
import org.sonarlint.eclipse.ui.internal.popup.ReleaseNotesPopup;
import org.sonarlint.eclipse.ui.internal.popup.TaintVulnerabilityAvailablePopup;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

public class SonarLintUiPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.ui";

  // The shared instance
  private static SonarLintUiPlugin plugin;

  private IPropertyChangeListener prefListener;

  private SonarLintConsoleLogger logListener;
  private PopupNotification notifListener;

  @Nullable
  private SonarLintConsole console;

  private static final WindowOpenCloseListener WINDOW_OPEN_CLOSE_LISTENER = new WindowOpenCloseListener();
  private static final SonarLintPostBuildListener SONARLINT_POST_BUILD_LISTENER = new SonarLintPostBuildListener();
  private static final SonarLintVcsCacheCleaner SONARLINT_VCS_CACHE_CLEANER = new SonarLintVcsCacheCleaner();
  private static final SonarLintFlowLocationsService SONARLINT_FLOW_LOCATION_SERVICE = new SonarLintFlowLocationsService();
  private static final SonarLintNoAutomaticBuildWarningService SONARLINT_AUTOMATIC_BUILD_SERVICE = new SonarLintNoAutomaticBuildWarningService();
  private static final SonarLintRpcClientSupportService SONARLINT_RPC_CLIENT_SUPPORT_SERVICE = new SonarLintRpcClientSupportService();
  private static final ConfigScopeIdCacheCleaner CONFIG_SCOPE_ID_CACHE_CLEANER = new ConfigScopeIdCacheCleaner();

  public SonarLintUiPlugin() {
    plugin = this;
  }

  private class SonarLintConsoleLogger implements LogListener {

    /**
     * We need to process logs asynchronously to not slow down the source of logs, and not lock the UI. Still we need to preserve log
     * ordering. So we don't use asyncExec, but instead use a single thread executor service + syncExec
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=421303
     */
    private final ExecutorService logConsumer = Executors.newSingleThreadExecutor(SonarLintUtils.threadFactory("sonarlint-log-consummer", true));

    @Override
    public void info(@Nullable String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        doAsyncInUiThread(() -> getSonarConsole().info(msg, fromAnalyzer));
      }
    }

    @Override
    public void error(@Nullable String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        doAsyncInUiThread(() -> getSonarConsole().error(msg, fromAnalyzer));
      }
    }

    @Override
    public void debug(@Nullable String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        doAsyncInUiThread(() -> getSonarConsole().debug(msg, fromAnalyzer));
      }
    }

    @Override
    public void traceIdeMessage(@Nullable String msg) {
      if (PlatformUI.isWorkbenchRunning()) {
        doAsyncInUiThread(() -> getSonarConsole().traceIdeMessage(msg));
      }
    }

    void doAsyncInUiThread(Runnable task) {
      logConsumer.submit(() -> Display.getDefault().syncExec(task));
    }

    public void shutdown() {
      logConsumer.shutdown();
    }

  }

  private static class PopupNotification implements NotificationListener {

    @Override
    public void showNotification(Notification notif) {
      Display.getDefault().asyncExec(() -> {
        var popup = new GenericNotificationPopup(notif.getTitle(), notif.getShortMsg(), notif.getLongMsg(),
          notif.getLearnMoreUrl());
        popup.open();
      });
    }
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    logListener = new SonarLintConsoleLogger();
    SonarLintLogger.get().addLogListener(logListener);

    notifListener = new PopupNotification();
    SonarLintNotifications.get().addNotificationListener(notifListener);

    SonarLintBackendService.get().init(new SonarLintEclipseRpcClient());

    addPostBuildListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(SONARLINT_VCS_CACHE_CLEANER);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(CONFIG_SCOPE_ID_CACHE_CLEANER);
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(SONARLINT_FLOW_LOCATION_SERVICE);
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(SONARLINT_AUTOMATIC_BUILD_SERVICE);
    SonarLintRpcClientSupportSynchronizer.addListener(SONARLINT_RPC_CLIENT_SUPPORT_SERVICE);

    prefListener = event -> {
      if (event.getProperty().equals(SonarLintGlobalConfiguration.PREF_MARKER_SEVERITY)) {
        try {
          MarkerUtils.updateAllSonarMarkerSeverity();
        } catch (CoreException e) {
          SonarLintLogger.get().error("Unable to update marker severity", e);
        }
      }
    };

    getPreferenceStore().addPropertyChangeListener(prefListener);

    SonarLintMarkerUpdater.setTaintVulnerabilitiesListener(SonarLintUiPlugin::notifyTaintVulnerabilitiesDisplayed);

    startupAsync();
  }

  private static void notifyTaintVulnerabilitiesDisplayed(boolean comeFromSonarCloud) {
    if (SonarLintGlobalConfiguration.taintVulnerabilityNeverBeenDisplayed()) {
      SonarLintGlobalConfiguration.setTaintVulnerabilityDisplayed();
      Display.getDefault().syncExec(() -> showTaintVulnerabitilityNotification(comeFromSonarCloud));
    }
  }

  private static void showTaintVulnerabitilityNotification(boolean comeFromSonarCloud) {
    new TaintVulnerabilityAvailablePopup(comeFromSonarCloud).open();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    removePostBuildListener();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(SONARLINT_VCS_CACHE_CLEANER);
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(CONFIG_SCOPE_ID_CACHE_CLEANER);
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(SONARLINT_FLOW_LOCATION_SERVICE);
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(SONARLINT_AUTOMATIC_BUILD_SERVICE);
    SonarLintRpcClientSupportSynchronizer.removeListener(SONARLINT_RPC_CLIENT_SUPPORT_SERVICE);
    SonarLintLogger.get().removeLogListener(logListener);
    logListener.shutdown();
    SonarLintNotifications.get().removeNotificationListener(notifListener);
    if (PlatformUI.isWorkbenchRunning()) {
      for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
        WindowOpenCloseListener.removeListenerFromAllPages(window);
      }
    }
    try {
      getPreferenceStore().removePropertyChangeListener(prefListener);
    } finally {
      super.stop(context);
    }
    SonarLintUiExtensionTracker.close();
  }

  public static void addPostBuildListener() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(SONARLINT_POST_BUILD_LISTENER, IResourceChangeEvent.POST_BUILD);
  }

  public static void removePostBuildListener() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(SONARLINT_POST_BUILD_LISTENER);
  }

  /**
   * @return the shared instance
   */
  public static SonarLintUiPlugin getDefault() {
    return plugin;
  }

  public synchronized SonarLintConsole getSonarConsole() {
    if (console != null) {
      return console;
    }
    // no console found, so create a new one
    console = new SonarLintConsole(SonarLintImages.SONARLINT_16);
    return console;
  }

  public synchronized void closeSonarConsole() {
    if (console != null) {
      var manager = ConsolePlugin.getDefault().getConsoleManager();
      manager.removeConsoles(new IConsole[] {console});
      this.console = null;
    }
  }

  /** We don't run an analysis of all opened files anymore as the backend needs to get ready first */
  private static class StartupJob extends Job {

    StartupJob() {
      super("SonarLint UI startup");
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      SonarLintLogger.get().info("Starting SonarLint for Eclipse " + SonarLintUtils.getPluginVersion());

      if (PlatformUI.isWorkbenchRunning()) {
        // Handle future opened/closed windows
        PlatformUI.getWorkbench().addWindowListener(WINDOW_OPEN_CLOSE_LISTENER);
        // Now we can attach listeners to existing windows
        for (var window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
          WindowOpenCloseListener.addListenerToAllPages(window);
        }

        // Check if updated or freshly installed and then show a notification raising awareness about the release notes
        // -> also open the "Welcome" view for users to get started
        if (!SonarLintGlobalConfiguration.sonarLintVersionHintHidden() && BundleUtils.bundleUpdatedOrInstalled()) {
          ReleaseNotesPopup.displayPopupIfNotAlreadyShown();
          PlatformUtils.openWelcomePage();
        }

        // Check if newer version is available and then show a notification raising awareness about it. The
        // notification will only be displayed once a day in order to not annoy the user!
        if (!SonarLintGlobalConfiguration.sonarLintVersionHintHidden()
          && !SonarLintGlobalConfiguration.isSonarLintVersionHintDateToday()) {
          var newestSonarLintVersion = EclipseUpdateSite.getNewestVersion();
          var currentSonarLintVersion = BundleUtils.getBundleVersion();
          SonarLintLogger.get().debug("Current version (" + currentSonarLintVersion + "), newest version ("
            + newestSonarLintVersion + ")");

          if (newestSonarLintVersion == null) {
            SonarLintLogger.get().debug("Cannot check for newer SonarLint versions via the SonarLint for Eclipse "
              + "Update Site. In this case, please check the Sonar Community Forum ("
              + SonarLintDocumentation.COMMUNITY_FORUM_ECLIPSE_RELEASES + ") or the GitHub releases page ("
              + SonarLintDocumentation.GITHUB_RELEASES + ")!");
          } else if (newestSonarLintVersion.isNewerThan(currentSonarLintVersion)) {
            NewerVersionAvailablePopup.displayPopupIfNotAlreadyShown(newestSonarLintVersion.toString());
          }
        }
      }

      // We want to update the locally saved SonarLint version reference once everything is done!
      SonarLintGlobalConfiguration.setSonarLintVersion();
      SonarLintGlobalConfiguration.setSonarLintVersionHintDate();

      // Display user survey pop-up (comment out if not needed, comment in again if needed and replace link)
      // Display.getDefault().syncExec(() -> SurveyPopup.displaySurveyPopupIfNotAlreadyAccessed(""));

      return Status.OK_STATUS;
    }

  }

  public void startupAsync() {
    // SLE-122 Delay a little bit to let the time to the workspace to initialize (and avoid NPE)
    new StartupJob().schedule(2000);
  }

  public static SonarLintFlowLocationsService getSonarlintMarkerSelectionService() {
    return SONARLINT_FLOW_LOCATION_SERVICE;
  }
}
