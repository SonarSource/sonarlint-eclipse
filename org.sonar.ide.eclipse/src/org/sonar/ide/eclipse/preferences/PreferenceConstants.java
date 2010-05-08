/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
