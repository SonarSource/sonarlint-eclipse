package org.maven.ide.eclipse.sonar;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.properties.ProjectProperties;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectConfigurator extends AbstractProjectConfigurator {
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    configureProject(request.getProject(), request.getMavenProject());
  }

  @Override
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    configureProject(facade.getProject(), facade.getMavenProject());
  }

  private void configureProject(IProject project, MavenProject mavenProject) {
    String groupId = mavenProject.getGroupId();
    String artifactId = mavenProject.getArtifactId();
    System.out.println(groupId + ":" + artifactId); // TODO remove
    ProjectProperties projectProperties = ProjectProperties.getInstance(project);
    projectProperties.setGroupId(groupId);
    projectProperties.setArtifactId(artifactId);
  }
}
