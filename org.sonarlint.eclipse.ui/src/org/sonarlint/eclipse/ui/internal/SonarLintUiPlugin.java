/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.console.SonarLintConsole;
import org.sonarlint.eclipse.ui.internal.job.CheckForUpdatesJob;
import org.sonarlint.eclipse.ui.internal.popup.ServerStorageNeedUpdatePopup;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;

public class SonarLintUiPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.ui";

  // The shared instance
  private static SonarLintUiPlugin plugin;

  private IPropertyChangeListener prefListener;

  private LogListener logListener;

  private SonarLintConsole console;

  private static final SonarLintPartListener SONARLINT_PART_LISTENER = new SonarLintPartListener();
  private static final SonarLintChangeListener SONARLINT_CHANGE_LISTENER = new SonarLintChangeListener();

  public SonarLintUiPlugin() {
    plugin = this;
  }

  private class SonarLintConsoleLogger implements LogListener {
    @Override
    public void info(String msg, boolean fromAnalyzer) {
      getSonarConsole().info(msg, fromAnalyzer);
    }

    @Override
    public void error(String msg, boolean fromAnalyzer) {
      getSonarConsole().error(msg, fromAnalyzer);
    }

    @Override
    public void debug(String msg, boolean fromAnalyzer) {
      getSonarConsole().debug(msg, fromAnalyzer);
    }
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    ResourcesPlugin.getWorkspace().addResourceChangeListener(SONARLINT_CHANGE_LISTENER, IResourceChangeEvent.POST_CHANGE);

    logListener = new SonarLintConsoleLogger();
    SonarLintLogger.get().addLogListener(logListener);

    addSonarLintPartListener();

    prefListener = event -> {
      if (event.getProperty().equals(PreferencesUtils.PREF_MARKER_SEVERITY)) {
        try {
          MarkerUtils.updateAllSonarMarkerSeverity();
        } catch (CoreException e) {
          SonarLintLogger.get().error("Unable to update marker severity", e);
        }
      }
    };

    getPreferenceStore().addPropertyChangeListener(prefListener);

    SonarLintLogger.get().info("Starting SonarLint for Eclipse " + getBundle().getVersion());

    checkServersStatus();

    new CheckForUpdatesJob().schedule((long) 10 * 1000);

    analyzeCurrentFile();

  }

  private static void checkServersStatus() {
    for (final IServer server : ServersManager.getInstance().getServers()) {
      if (!server.isStorageUpdated()) {
        Display.getDefault().asyncExec(() -> {
          ServerStorageNeedUpdatePopup popup = new ServerStorageNeedUpdatePopup(Display.getCurrent(), server);
          popup.create();
          popup.open();
        });
      }
    }
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(SONARLINT_CHANGE_LISTENER);
    SonarLintLogger.get().removeLogListener(logListener);
    try {
      getPreferenceStore().removePropertyChangeListener(prefListener);
    } finally {
      super.stop(context);
    }
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

  /**
   * Initializes a preference store with default preference values
   * for this plug-in.
   */
  @Override
  protected void initializeDefaultPreferences(IPreferenceStore store) {
    store.setDefault(SonarLintConsole.P_SHOW_CONSOLE, SonarLintConsole.P_SHOW_CONSOLE_ON_ERROR);
    store.setDefault(SonarLintConsole.P_ANALYZER_OUTPUT, false);
    store.setDefault(PreferencesUtils.PREF_MARKER_SEVERITY, PreferencesUtils.PREF_MARKER_SEVERITY_DEFAULT);
    store.setDefault(PreferencesUtils.PREF_EXTRA_ARGS, PreferencesUtils.PREF_EXTRA_ARGS_DEFAULT);
    store.setDefault(PreferencesUtils.PREF_TEST_FILE_REGEXPS, PreferencesUtils.PREF_TEST_FILE_REGEXPS_DEFAULT);
  }

  private static class RegisterSonarLintPartListenerJob extends UIJob {
    RegisterSonarLintPartListenerJob() {
      super("Register SonarLint part listener");
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
        addListenerToAllPages(window);
      }

      // Handle future opened/closed windows
      PlatformUI.getWorkbench().addWindowListener(new WindowOpenCloseListener());

      return Status.OK_STATUS;
    }

    static class WindowOpenCloseListener implements IWindowListener {
      @Override
      public void windowOpened(IWorkbenchWindow window) {
        addListenerToAllPages(window);
      }

      @Override
      public void windowDeactivated(IWorkbenchWindow window) {
        // Nothing to do
      }

      @Override
      public void windowClosed(IWorkbenchWindow window) {
        removeListenerToAllPages(window);
      }

      @Override
      public void windowActivated(IWorkbenchWindow window) {
        // Nothing to do
      }

      private static void removeListenerToAllPages(IWorkbenchWindow window) {
        for (IWorkbenchPage page : window.getPages()) {
          page.removePartListener(SONARLINT_PART_LISTENER);
        }
      }
    }

    private static void addListenerToAllPages(IWorkbenchWindow window) {
      for (IWorkbenchPage page : window.getPages()) {
        page.addPartListener(SONARLINT_PART_LISTENER);
      }
    }
  }

  private static class AnalyzeCurrentFileJob extends UIJob {

    AnalyzeCurrentFileJob() {
      super("Analyze current file");
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {

      JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STARTUP);

      return Status.OK_STATUS;
    }

  }

  @CheckForNull
  public static ISonarLintFile findCurrentEditedFile() {
    IEditorPart editor = findActiveEditor();
    if (editor == null) {
      return null;
    }
    // note: the cast is necessary for e43 and e44
    IFile file = (IFile) editor.getEditorInput().getAdapter(IFile.class);
    if (file != null) {
      return (ISonarLintFile) file.getAdapter(ISonarLintFile.class);
    }
    return null;
  }

  @CheckForNull
  public static IEditorPart findActiveEditor() {
    // Super defensing programming because we don't really understand what is initialized at startup (SLE-122)
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return null;
    }
    return page.getActiveEditor();
  }

  public static void analyzeCurrentFile() {
    // SLE-122 Delay a little bit to let the time to the workspace to initialize (and avoid NPE)
    new AnalyzeCurrentFileJob().schedule(2000);
  }

  public static void addSonarLintPartListener() {
    new RegisterSonarLintPartListenerJob().schedule();
  }

}
