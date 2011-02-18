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
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
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

  protected IProfileVersioner profileVersioner;
  protected ProfileStore profileStore;
  protected PreferencesAccess access;
  protected IScopeContext instanceScope;
  protected ProfileManager profileManager;
  private final Profile profile;
  private final Map workingValues;

  public ProfileConfiguration(String profileName, IProject project) {
    createProfileManager(project);
    if ( profileManager.containsName(profileName)) {
      profileManager.deleteProfile((CustomProfile) getProfile(profileName));
    }
    if ( !profileManager.containsName(profileName)) {
      IProfileVersioner profileVersioner = profileManager.getProfileVersioner();
      this.profile = new CustomProfile(profileName, profileManager.getDefaultProfile().getSettings(), profileVersioner.getCurrentVersion(),
          profileVersioner.getProfileKind());
      profileManager.addProfile((CustomProfile) profile);
    } else {
      this.profile = getProfile(profileName);
    }
    this.workingValues = new HashMap(profile.getSettings());
  }

  private void createProfileManager(IProject project) {
    this.profileVersioner = new ProfileVersioner();
    this.profileStore = new FormatterProfileStore(profileVersioner);
    this.access = PreferencesAccess.getOriginalPreferences(); // PreferencesAccess.getWorkingCopyPreferences(new WorkingCopyManager());
    this.instanceScope = access.getProjectScope(project);
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

    this.profileManager = new FormatterProfileManager(profiles, instanceScope, access, profileVersioner);
  }

  private Profile getProfile(String profileName) {
    for (Iterator iterator = profileManager.getSortedProfiles().iterator(); iterator.hasNext();) {
      Profile p = (Profile) iterator.next();
      if (p.getName().equals(profileName))
        return p;
    }
    return null;
  }

  public void add(String key, String value) {
    workingValues.put(key, value);
  }

  public void apply() {
    if (hasChanges()) {
      try {
        profile.setSettings(new HashMap(workingValues));
        profileManager.setSelected(profile);
        profileStore.writeProfiles(profileManager.getSortedProfiles(), instanceScope);
        profileManager.commitChanges(instanceScope);
      } catch (Exception e) {
        SonarJdtPlugin.log(e);
      }
    }
  }

  private boolean hasChanges() {
    Iterator iter = profile.getSettings().entrySet().iterator();
    for (; iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      if ( workingValues.get(entry.getKey())!= entry.getValue()) {
        return true;
      }
    }
    return false;
  }
}
