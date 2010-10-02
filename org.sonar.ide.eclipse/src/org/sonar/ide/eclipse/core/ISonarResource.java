package org.sonar.ide.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * Represents a view of a resource in terms of Sonar.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarResource {

  /**
   * @return resource key
   */
  String getKey();

  /**
   * @return project which contains this resource
   */
  IProject getProject();

  /**
   * @return resource associated with this Sonar resource
   */
  IResource getResource();

}
