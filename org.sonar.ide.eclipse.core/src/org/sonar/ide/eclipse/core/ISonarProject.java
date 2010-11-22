package org.sonar.ide.eclipse.core;

/**
 * Represents a view of a project in terms of Sonar.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarProject extends ISonarResource {

  String getGroupId();

  String getArtifactId();

  String getBranch();

}
