package org.sonar.ide.eclipse.ui.tests.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.sonar.ide.eclipse.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.properties.ProjectProperties;

public class ProjectUtils {

  public static void configureProject(String name, String url) throws Exception {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    configureProject(project, url, "org.sonar-ide.tests.SimpleProject", name);
  }

  // TODO should be in core
  public static void configureProject(IProject project, String url, String groupId, String artifactId) throws Exception {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId(groupId);
    properties.setArtifactId(artifactId);
    properties.save();
    ToggleNatureAction.enableNature(project);
  }

}
