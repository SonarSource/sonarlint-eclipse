/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.jdt.profiles;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.internal.jdt.SonarJdtPlugin;
import org.sonar.ide.eclipse.internal.ui.jobs.AbstractRemoteSonarJob;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
public class RetrieveSonarProfileJob extends AbstractRemoteSonarJob {

  protected IProfileVersioner profileVersioner;
  protected ProfileStore profileStore;
  protected PreferencesAccess access;
  protected IScopeContext instanceScope;
  protected ProfileManager profileManager;

  public RetrieveSonarProfileJob() {
    super("Retrieve sonar profile");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    profileVersioner =  new ProfileVersioner();
    profileStore = new FormatterProfileStore(profileVersioner);
    access = PreferencesAccess.getOriginalPreferences(); // PreferencesAccess.getWorkingCopyPreferences(new WorkingCopyManager());

    instanceScope = access.getInstanceScope();
    List profiles = null;
    try {
      profiles = profileStore.readProfiles(instanceScope);
    } catch (CoreException e) {
      SonarJdtPlugin.log(e);
    }
    if (profiles == null) {
      try {
        // bug 129427
        profiles = profileStore.readProfiles(new DefaultScope());
      } catch (CoreException e) {
        JavaPlugin.log(e);
      }
    }

    if (profiles == null)
      profiles = new ArrayList();

    profileManager =  new FormatterProfileManager(profiles, instanceScope, access, profileVersioner);

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (int i = 0; i < projects.length; i++) {
      if ( !monitor.isCanceled() && projects[i].isAccessible()) {
        EclipseSonar index = EclipseSonar.getInstance(projects[i]);
        SourceCode sourceCode = index.search(projects[i]);
        if (sourceCode != null) {
          final Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(sourceCode.getKey(), "profile" /*
                                                                                                                         * TODO :
                                                                                                                         * ProfileUtil
                                                                                                                         * .METRIC_KEY
                                                                                                                         */));
          final Measure measure = resource.getMeasure("profile" /* TODO : ProfileUtil.METRIC_KEY */);
          if (measure != null) {
            create(measure.getData());
          }
        }
      }
    }
    return Status.OK_STATUS;
  }

  private void create(String profileName) {

    if ( !profileManager.containsName(profileName)) {
      CustomProfile profile = new CustomProfile(profileName, profileManager.getDefaultProfile().getSettings(),
          profileVersioner.getCurrentVersion(), profileVersioner.getProfileKind());
      profileManager.addProfile(profile);
    }
    try {
      profileStore.writeProfiles(profileManager.getSortedProfiles(), instanceScope); // update profile store
      profileManager.commitChanges(instanceScope);
    } catch (CoreException x) {
      JavaPlugin.log(x);
    }
  }
}
