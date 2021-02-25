/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.notifications.ListenerFactory;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.console.SonarLintConsole;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintFlowLocationsService;
import org.sonarlint.eclipse.ui.internal.hotspots.SecurityHotspotsHandlerServer;
import org.sonarlint.eclipse.ui.internal.job.CheckForUpdatesJob;
import org.sonarlint.eclipse.ui.internal.popup.DeveloperNotificationPopup;
import org.sonarlint.eclipse.ui.internal.popup.GenericNotificationPopup;
import org.sonarlint.eclipse.ui.internal.popup.MissingNodePopup;
import org.sonarlint.eclipse.ui.internal.popup.ServerStorageNeedUpdatePopup;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotification;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotificationListener;

public class SonarLintUiPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.ui";

  // The shared instance
  private static SonarLintUiPlugin plugin;

  private IPropertyChangeListener prefListener;

  private LogListener logListener;
  private PopupNotification notifListener;

  private SonarLintConsole console;

  private ListenerFactory listenerFactory;

  private final SecurityHotspotsHandlerServer hotspotsHandlerServer = new SecurityHotspotsHandlerServer();

  private static final WindowOpenCloseListener WINDOW_OPEN_CLOSE_LISTENER = new WindowOpenCloseListener();
  private static final SonarLintPostBuildListener SONARLINT_POST_BUILD_LISTENER = new SonarLintPostBuildListener();
  private static final SonarLintProjectEventListener SONARLINT_PROJECT_EVENT_LISTENER = new SonarLintProjectEventListener();
  private static final SonarLintFlowLocationsService SONARLINT_FLOW_LOCATION_SERVICE = new SonarLintFlowLocationsService();

  public SonarLintUiPlugin() {
    plugin = this;
  }

  private class SonarLintConsoleLogger implements LogListener {
    @Override
    public void info(String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        getSonarConsole().info(msg, fromAnalyzer);
      }
    }

    @Override
    public void error(String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        if (isNodeCommandException(msg)) {
          getSonarConsole().info(msg, false);
          Display.getDefault().asyncExec(() -> {
            MissingNodePopup popup = new MissingNodePopup();
            popup.setFadingEnabled(false);
            popup.setDelayClose(0L);
            popup.open();
          });
        } else {
          getSonarConsole().error(msg, fromAnalyzer);
        }
      }
    }

    private boolean isNodeCommandException(String msg) {
      return msg.contains("NodeCommandException");
    }

    @Override
    public void debug(String msg, boolean fromAnalyzer) {
      if (PlatformUI.isWorkbenchRunning()) {
        getSonarConsole().debug(msg, fromAnalyzer);
      }
    }

  }

  private static class PopupNotification implements NotificationListener {

    @Override
    public void showNotification(Notification notif) {
      Display.getDefault().asyncExec(() -> {
        GenericNotificationPopup popup = new GenericNotificationPopup(notif.getTitle(), notif.getShortMsg(), notif.getLongMsg());
        popup.open();
      });
    }
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    addPostBuildListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(SONARLINT_PROJECT_EVENT_LISTENER);
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(SONARLINT_FLOW_LOCATION_SERVICE);

    logListener = new SonarLintConsoleLogger();
    SonarLintLogger.get().addLogListener(logListener);

    notifListener = new PopupNotification();
    SonarLintNotifications.get().addNotificationListener(notifListener);

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

    new CheckForUpdatesJob().schedule((long) 10 * 1000);

    startupAsync();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    hotspotsHandlerServer.shutdown();
    removePostBuildListener();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(SONARLINT_PROJECT_EVENT_LISTENER);
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(SONARLINT_FLOW_LOCATION_SERVICE);
    SonarLintLogger.get().removeLogListener(logListener);
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
    console = new SonarLintConsole(SonarLintImages.SONARLINT_CONSOLE_IMG_DESC);
    return console;
  }

  public synchronized void closeSonarConsole() {
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      manager.removeConsoles(new IConsole[] {console});
      this.console = null;
    }
  }

  public synchronized ListenerFactory listenerFactory() {
    if (listenerFactory == null) {
      // don't replace the anon class with lambda, because then the factory's "create" will always return the same listener instance
      listenerFactory = (IConnectedEngineFacade s) -> new ServerNotificationListener() {
        @Override
        public void handle(ServerNotification notification) {
          Display.getDefault().asyncExec(() -> {
            DeveloperNotificationPopup popup = new DeveloperNotificationPopup(s, notification, s.isSonarCloud());
            popup.open();
            SonarLintTelemetry telemetry = SonarLintCorePlugin.getTelemetry();
            telemetry.devNotificationsReceived(notification.category());
          });
        }
      };
    }
    return listenerFactory;
  }

  private class StartupJob extends Job {

    StartupJob() {
      super("SonarLint UI startup");
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      SonarLintLogger.get().info("Starting SonarLint for Eclipse " + SonarLintUtils.getPluginVersion());

      checkServersStatus();

      JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STARTUP);

      if (PlatformUI.isWorkbenchRunning()) {
        // Handle future opened/closed windows
        PlatformUI.getWorkbench().addWindowListener(WINDOW_OPEN_CLOSE_LISTENER);
        // Now we can attach listeners to existing windows
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
          WindowOpenCloseListener.addListenerToAllPages(window);
        }
      }

      SonarLintCorePlugin.getInstance().notificationsManager().subscribeAllNeedingProjectsToNotifications(SonarLintUiPlugin.getDefault().listenerFactory());

      hotspotsHandlerServer.init();

      return Status.OK_STATUS;
    }

    private void checkServersStatus() {
      for (final IConnectedEngineFacade server : SonarLintCorePlugin.getServersManager().getServers()) {
        if (server.getStorageState() != State.UPDATED) {
          Display.getDefault().asyncExec(() -> {
            ServerStorageNeedUpdatePopup popup = new ServerStorageNeedUpdatePopup(server);
            popup.open();
          });
        }
      }
    }
  }

  public void startupAsync() {
    // SLE-122 Delay a little bit to let the time to the workspace to initialize (and avoid NPE)
    new StartupJob().schedule(2000);
  }

  public static void unsubscribeToNotifications(ISonarLintProject project) {
    SonarLintCorePlugin.getInstance().notificationsManager().unsubscribe(project);
  }

  public static SonarLintFlowLocationsService getSonarlintMarkerSelectionService() {
    return SONARLINT_FLOW_LOCATION_SERVICE;
  }
}
