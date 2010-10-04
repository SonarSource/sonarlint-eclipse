package org.sonar.ide.eclipse.core;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarConstants {

  String PLUGIN_ID = "org.sonar.ide.eclipse";

  String NATURE_ID = PLUGIN_ID + ".sonarNature";

  /**
   * Godin: It would be better to use only one MARKER_ID at least at first time.
   */
  String MARKER_ID = PLUGIN_ID + ".sonarProblem";

  // TODO change value to .sonarPerspective
  String PERSPECTIVE_ID = PLUGIN_ID + ".perspectives.SonarPerspective";

}
