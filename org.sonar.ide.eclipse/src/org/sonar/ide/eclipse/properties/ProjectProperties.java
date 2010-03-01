package org.sonar.ide.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jérémie Lagarde
 */
public class ProjectProperties {

  private static final String P_SONAR_SERVER_URL = "sonarServerUrl";
  private static final String P_SONAR_SERVER_USERNAME = "sonarServerUsername";
  private static final String P_SONAR_SERVER_PASSWORD = "sonarServerPassword";
  private static final String P_PROJECT_GROUPID = "projectGroupId";
  private static final String P_PROJECT_ARTIFACTID = "projectArtifactId";

  private static Map<IProject, ProjectProperties> projectPropertiesMap = new HashMap<IProject, ProjectProperties>();

  private IProject project = null;
  private IEclipsePreferences preferences = null;

  protected ProjectProperties(IProject project) {
    this.project = project;
    initPreferencesStore();
  }

  private void initPreferencesStore() {
    IScopeContext projectScope = new ProjectScope(project);
    preferences = projectScope.getNode(SonarPlugin.PLUGIN_ID);
  }

  public static ProjectProperties getInstance(IResource resource) {
    if (resource == null) {
      return null;
    }
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      return null;
    }
    ProjectProperties projectProperties = projectPropertiesMap.get(project);
    if (projectProperties != null) {
      return projectProperties;
    }
    projectProperties = new ProjectProperties(project);
    projectPropertiesMap.put(project, projectProperties);
    return projectProperties;
  }

  public String getUrl() {
    return preferences.get(P_SONAR_SERVER_URL, SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL));
  }

  public void setUrl(String url) {
    preferences.put(P_SONAR_SERVER_URL, url);
  }

  public String getUsername() {
    return preferences.get(P_SONAR_SERVER_USERNAME, "");
  }

  public void setUsername(String username) {
    preferences.put(P_SONAR_SERVER_USERNAME, username);
  }

  public String getPassword() {
    return preferences.get(P_SONAR_SERVER_PASSWORD, "");
  }

  public void setPassword(String password) {
    preferences.put(P_SONAR_SERVER_PASSWORD, password);
  }

  public String getGroupId() {
    return preferences.get(P_PROJECT_GROUPID, "");
  }

  public void setGroupId(String groupId) {
    preferences.put(P_PROJECT_GROUPID, groupId);
  }

  public String getArtifactId() {
    return preferences.get(P_PROJECT_ARTIFACTID, project.getName());
  }

  public void setArtifactId(String artifactId) {
    preferences.put(P_PROJECT_ARTIFACTID, artifactId);
  }

  public void flush() throws BackingStoreException {
    preferences.flush();
  }

}
