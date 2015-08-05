/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.core.internal.PreferencesUtils;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.ui.internal.console.SonarConsole;

public class SonarUiPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.ui";

  // The shared instance
  private static SonarUiPlugin plugin;

  private IPropertyChangeListener listener;

  public SonarUiPlugin() {
    plugin = this;
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    if (getSonarConsole() != null) {
      SonarCorePlugin.getDefault().addLogListener(getSonarConsole());
    }

    setupIssuesUpdater();

    listener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(PreferencesUtils.PREF_MARKER_SEVERITY) || event.getProperty().equals(PreferencesUtils.PREF_NEW_ISSUE_MARKER_SEVERITY)) {
          try {
            MarkerUtils.updateAllSonarMarkerSeverity();
          } catch (CoreException e) {
            SonarCorePlugin.getDefault().error("Unable to update marker severity", e);
          }
        }
      }
    };
    getPreferenceStore().addPropertyChangeListener(listener);
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    try {
      if (getSonarConsole() != null) {
        SonarCorePlugin.getDefault().removeLogListener(getSonarConsole());
      }
      getPreferenceStore().removePropertyChangeListener(listener);
    } finally {
      super.stop(context);
    }
  }

  /**
   * @return the shared instance
   */
  public static SonarUiPlugin getDefault() {
    return plugin;
  }

  private SonarConsole console;

  public synchronized SonarConsole getSonarConsole() {
    // Don't try to initialize console without actual UI - it will cause headless tests failure
    if ((console == null) && PlatformUI.isWorkbenchRunning()) {
      console = new SonarConsole(SonarImages.SONAR16_IMG);
    }
    return console;
  }

  /**
   * Initializes a preference store with default preference values
   * for this plug-in.
   */
  @Override
  protected void initializeDefaultPreferences(IPreferenceStore store) {
    store.setDefault(SonarConsole.P_SHOW_CONSOLE, SonarConsole.P_SHOW_CONSOLE_ON_ERROR);
    store.setDefault(PreferencesUtils.PREF_MARKER_SEVERITY, IMarker.SEVERITY_WARNING);
    store.setDefault(PreferencesUtils.PREF_NEW_ISSUE_MARKER_SEVERITY, IMarker.SEVERITY_ERROR);
    store.setDefault(PreferencesUtils.PREF_EXTRA_ARGS, PreferencesUtils.PREF_EXTRA_ARGS_DEFAULT);
    store.setDefault(PreferencesUtils.PREF_JVM_ARGS, PreferencesUtils.PREF_JVM_ARGS_DEFAULT);
    store.setDefault(PreferencesUtils.PREF_FORCE_FULL_PREVIEW, PreferencesUtils.PREF_FORCE_FULL_PREVIEW_DEFAULT);
    store.setDefault(PreferencesUtils.PREF_TEST_FILE_REGEXPS, PreferencesUtils.PREF_TEST_FILE_REGEXPS_DEFAULT);
  }

  public static void setupIssuesUpdater() {
    final IssuesUpdater issuesUpdater = new IssuesUpdater();
    new UIJob("Prepare issues updater") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        final IWorkbenchPage page = SonarUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.addPartListener(issuesUpdater);
        return Status.OK_STATUS;
      }
    }.schedule();
  }

}
