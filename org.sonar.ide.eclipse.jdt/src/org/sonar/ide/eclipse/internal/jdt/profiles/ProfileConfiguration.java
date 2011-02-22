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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.sonar.ide.eclipse.internal.jdt.SonarJdtPlugin;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
@SuppressWarnings("restriction")
public class ProfileConfiguration {
  protected ProfileStore formatterProfileStore;
  protected ProfileStore cleanUpprofileStore;
  protected PreferencesAccess access;
  protected IScopeContext instanceScope;
  protected IScopeContext projectScope;
  protected ProfileManager formatterProfileManager;
  protected ProfileManager cleanUpProfileManager;
  private final Profile profile;
  private final Map workingValues;

  public ProfileConfiguration(String profileName, IProject project) {
    createProfileManager(project);
    if(formatterProfileManager.containsName(profileName)) {
      formatterProfileManager.deleteProfile((CustomProfile) getProfile(profileName));
    }
    if( !formatterProfileManager.containsName(profileName)) {
      IProfileVersioner profileVersioner = formatterProfileManager.getProfileVersioner();
      this.profile = new CustomProfile(profileName, formatterProfileManager.getDefaultProfile().getSettings(),
          profileVersioner.getCurrentVersion(), profileVersioner.getProfileKind());
      formatterProfileManager.addProfile((CustomProfile) profile);
    } else {
      this.profile = getProfile(profileName);
    }

    if( !cleanUpProfileManager.containsName(profileName)) {
      cleanUpProfileManager.addProfile((CustomProfile) profile);
    }

    this.workingValues = new HashMap(profile.getSettings());
  }

  private void createProfileManager(IProject project) {
    ProfileVersioner formatterProfileVersioner = new ProfileVersioner();
    this.formatterProfileStore = new FormatterProfileStore(formatterProfileVersioner);
    CleanUpProfileVersioner cleanUpProfileVersioner = new CleanUpProfileVersioner();
    this.cleanUpprofileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES,cleanUpProfileVersioner);
    this.access = PreferencesAccess.getOriginalPreferences(); // PreferencesAccess.getWorkingCopyPreferences(new WorkingCopyManager());
    this.instanceScope = access.getInstanceScope();
    this.projectScope = access.getProjectScope(project);
    List profiles = null;
    try {
      profiles = formatterProfileStore.readProfiles(instanceScope);
    } catch(CoreException e) {
      SonarJdtPlugin.log(e);
    }
    if(profiles == null) {
      try {
        // bug 129427
        profiles = formatterProfileStore.readProfiles(new DefaultScope());
      } catch(CoreException e) {
        JavaPlugin.log(e);
      }
    }

    if(profiles == null)
      profiles = new ArrayList();

    this.formatterProfileManager = new FormatterProfileManager(profiles, instanceScope, access, formatterProfileVersioner);
    this.cleanUpProfileManager = new CleanUpProfileManager(CleanUpPreferenceUtil.getBuiltInProfiles(), instanceScope, access,
        cleanUpProfileVersioner);
  }

  private Profile getProfile(String profileName) {
    for(Iterator iterator = formatterProfileManager.getSortedProfiles().iterator(); iterator.hasNext();) {
      Profile p = (Profile) iterator.next();
      if(p.getName().equals(profileName))
        return p;
    }
    return null;
  }

  public void add(String key, String value) {
    workingValues.put(key, value);
  }

  public void apply() {
    if(hasChanges()) {
      try {
        profile.setSettings(new HashMap(workingValues));
        formatterProfileManager.setSelected(profile);
        cleanUpProfileManager.setSelected(profile);
        formatterProfileStore.writeProfiles(formatterProfileManager.getSortedProfiles(), instanceScope);
        cleanUpprofileStore.writeProfiles(cleanUpProfileManager.getSortedProfiles(), instanceScope);
        // Save as global preferences.
        formatterProfileManager.commitChanges(instanceScope);
        cleanUpProfileManager.commitChanges(instanceScope);
        // Save as project preferences.
        formatterProfileManager.commitChanges(projectScope);
        cleanUpProfileManager.commitChanges(projectScope);
      } catch(Exception e) {
        SonarJdtPlugin.log(e);
      }
    }
  }

  private boolean hasChanges() {
    Iterator iter = profile.getSettings().entrySet().iterator();
    for(; iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      if(workingValues.get(entry.getKey()) != entry.getValue()) {
        return true;
      }
    }
    return false;
  }
}
