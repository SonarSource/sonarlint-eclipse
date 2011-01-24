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
//
//import static org.hamcrest.CoreMatchers.is;
//import static org.junit.Assert.assertThat;
//import static org.junit.Assert.fail;
//
//import java.util.ArrayList;
//import java.util.List;
//
import org.eclipse.core.resources.IProject;
//import org.eclipse.core.runtime.NullProgressMonitor;
//import org.eclipse.core.runtime.jobs.Job;
//import org.eclipse.core.runtime.preferences.IScopeContext;
//import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
//import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
//import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
//import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
//import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
//import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
//import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
//import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.junit.Test;
//import org.sonar.ide.eclipse.internal.core.ISonarConstants;
//import org.sonar.ide.eclipse.internal.ui.actions.ToggleNatureAction;
//import org.sonar.ide.eclipse.internal.ui.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
public class RetrieveSonarProfileJobTest extends SonarTestCase {

  private static final String groupId = "org.sonar-ide.tests.profile";
  private static final String artifactId = "profile";
  private static IProject project;

  @Test
  public void testImportProfile() throws Exception {
//    IProject project = getProject();
//    try {
//      Job job = new RetrieveSonarProfileJob();
//      job.schedule();
//      Job.getJobManager().join(ISonarConstants.REMOTE_SONAR_JOB_FAMILY, new NullProgressMonitor());
//      // waitForJobs();
//    } catch (Throwable e) {
//      fail("Updating sonar profile with error : " + e.getMessage());
//    }
//
//    PreferencesAccess access = PreferencesAccess.getOriginalPreferences();
//
//    IScopeContext instanceScope = access.getInstanceScope();
//    IScopeContext scope = access.getProjectScope(project);
//
//    IProfileVersioner versioner = new ProfileVersioner();
//    ProfileStore profilesStore = new FormatterProfileStore(versioner);
//    List<Profile> profiles = profilesStore.readProfiles(instanceScope);
//
//    if (profiles == null) {
//      profiles = new ArrayList<Profile>();
//    }
//
//    ProfileManager manager = new FormatterProfileManager(profiles, scope, access, versioner);
//
//    Profile profile = manager.getSelected();
//    
//    assertThat(profile.getID(), is("_Sonar for Sonar"));
//    assertThat(profile.getName(), is("Sonar for Sonar"));
//  }
//
//  private IProject getProject() throws Exception {
//    if (project == null) {
//      project = importEclipseProject(artifactId);
//
//      // Configure the project
//      ProjectProperties properties = ProjectProperties.getInstance(project);
//      properties.setUrl(startTestServer());
//      properties.setGroupId(groupId);
//      properties.setArtifactId(artifactId);
//      properties.save();
//      ToggleNatureAction.enableNature(project);
//    }
//    return project;

  }
}
