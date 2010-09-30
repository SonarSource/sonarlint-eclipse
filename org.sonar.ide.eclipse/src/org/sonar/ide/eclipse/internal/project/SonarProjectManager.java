package org.sonar.ide.eclipse.internal.project;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonar.ide.api.Logs;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.properties.ProjectProperties;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectManager {

  private static final String P_VERSION = "version";
  private static final String P_SONAR_SERVER_URL = "sonarServerUrl";
  private static final String P_PROJECT_GROUPID = "projectGroupId";
  private static final String P_PROJECT_ARTIFACTID = "projectArtifactId";
  private static final String P_PROJECT_BRANCH = "projectBranch";

  private static final String VERSION = "1";

  public ProjectProperties readSonarConfiguration(IProject project) {
    Logs.INFO.debug("Rading configuration for project " + project.getName());
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(ISonarConstants.PLUGIN_ID);
    if (projectNode == null) {
      Logs.INFO.warn("Unable to read configuration");
      return new ProjectProperties(project);
    }
    String version = projectNode.get(P_VERSION, null);
    // Godin: we can perform migration here
    String artifactId = projectNode.get(P_PROJECT_ARTIFACTID, "");
    if (version == null) {
      if (StringUtils.isBlank(artifactId)) {
        artifactId = project.getName();
      }
    }

    ProjectProperties configuration = new ProjectProperties(project);
    configuration.setUrl(projectNode.get(P_SONAR_SERVER_URL, ""));
    configuration.setGroupId(projectNode.get(P_PROJECT_GROUPID, ""));
    configuration.setArtifactId(artifactId);
    configuration.setBranch(projectNode.get(P_PROJECT_BRANCH, ""));
    return configuration;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarConfiguration(IProject project, ProjectProperties configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(ISonarConstants.PLUGIN_ID);
    if (projectNode != null) {
      Logs.INFO.debug("Saving configuration for project " + project.getName());
      projectNode.put(P_VERSION, VERSION);

      projectNode.put(P_SONAR_SERVER_URL, configuration.getUrl());
      projectNode.put(P_PROJECT_GROUPID, configuration.getGroupId());
      projectNode.put(P_PROJECT_ARTIFACTID, configuration.getArtifactId());
      projectNode.put(P_PROJECT_BRANCH, configuration.getBranch());

      try {
        projectNode.flush();
        return true;
      } catch (BackingStoreException e) {
        Logs.INFO.error("Failed to save project configuration", e);
      }
    }
    return false;
  }

}
