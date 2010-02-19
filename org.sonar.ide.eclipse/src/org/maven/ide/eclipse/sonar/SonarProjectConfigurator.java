package org.maven.ide.eclipse.sonar;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectConfigurator extends AbstractProjectConfigurator {
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    MavenProject project = request.getMavenProject();
    configureSonar(project);
  }

  @Override
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    MavenProject project = facade.getMavenProject();
    configureSonar(project);
  }

  private void configureSonar(MavenProject project) {
    String groupId = project.getGroupId();
    String artifactId = project.getArtifactId();
    System.out.println(groupId + ":" + artifactId);
  }
}
