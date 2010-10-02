package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.eclipse.core.ISonarResource;

public class SonarResource implements ISonarResource {

  private IResource resource;
  private String key;

  public SonarResource(IResource resource, String key) {
    this.key = key;
    this.resource = resource;
  }

  public String getKey() {
    return key;
  }

  public IProject getProject() {
    return resource.getProject();
  }

  public IResource getResource() {
    return resource;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SonarResource) && (key.equals(((SonarResource) obj).key));
  }

  @Override
  public String toString() {
    return "SonarResource [key=" + key + "]";
  }

}
