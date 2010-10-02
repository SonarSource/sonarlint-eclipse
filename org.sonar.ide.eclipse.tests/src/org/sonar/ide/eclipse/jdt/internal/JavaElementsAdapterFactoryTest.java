package org.sonar.ide.eclipse.jdt.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

public class JavaElementsAdapterFactoryTest extends SonarTestCase {

  @Test
  public void shouldAdaptResourceToSonar() throws Exception {
    IProject project = importEclipseProject("SimpleProject");
    // Configure the project
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl("http://localhost:9000");
    String groupId = "org.sonar-ide.tests.SimpleProject";
    String artifactId = "SimpleProject";
    properties.setGroupId(groupId);
    properties.setArtifactId(artifactId);
    properties.save();

    JavaElementsAdapterFactory factory = new JavaElementsAdapterFactory();

    {
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(project, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId));
    }

    {
      IFolder folder = project.getFolder("src/main/java");
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(folder, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default]"));
    }

    {
      IFile file = project.getFile("src/main/java/ViolationOnFile.java");
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(file, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default].ViolationOnFile"));
    }
  }
}
