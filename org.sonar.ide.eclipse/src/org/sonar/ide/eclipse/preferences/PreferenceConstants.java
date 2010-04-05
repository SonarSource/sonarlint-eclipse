package org.sonar.ide.eclipse.preferences;

/**
 * Constant definitions for plug-in preferences.
 * 
 * @author Jérémie Lagarde
 */
public class PreferenceConstants {

  // Default Sonar Server
  public static final String P_SONAR_SERVER_URL         = "sonarServerUrlPreference"; //$NON-NLS-1$
  public static final String P_SONAR_SERVER_URL_DEFAULT = "http://localhost:9000/";  //$NON-NLS-1$

  // Console configuration
  public static final String P_CONSOLE_REQUEST_COLOR    = "consoleRequestColor";     //$NON-NLS-1$
  public static final String P_CONSOLE_RESPONSE_COLOR   = "consoleResponseColor";    //$NON-NLS-1$
  public static final String P_CONSOLE_ERROR_COLOR      = "consoleErrorColor";       //$NON-NLS-1$
  public static final String P_CONSOLE_LIMIT_OUTPUT     = "consoleLimitOutput";      //$NON-NLS-1$
  public static final String P_CONSOLE_HIGH_WATER_MARK  = "consoleHighWaterMark";    //$NON-NLS-1$
  public static final String P_CONSOLE_SHOW_ON_MESSAGE  = "consoleShowOnMessage";    //$NON-NLS-1$
  public static final String P_CONSOLE_SHOW_ON_ERROR    = "consoleShowOnError";      //$NON-NLS-1$

}
