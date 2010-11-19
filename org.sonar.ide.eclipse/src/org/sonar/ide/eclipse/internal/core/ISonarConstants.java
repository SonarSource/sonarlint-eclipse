/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.core;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarConstants {

  String PLUGIN_ID = "org.sonar.ide.eclipse";

  String NATURE_ID = PLUGIN_ID + ".sonarNature";

  String BUILDER_ID = PLUGIN_ID + ".sonarBuilder";

  /**
   * Godin: It would be better to use only one MARKER_ID at least at first time.
   */
  String MARKER_ID = PLUGIN_ID + ".sonarProblem";

  // TODO change value to .sonarPerspective
  String PERSPECTIVE_ID = PLUGIN_ID + ".perspectives.SonarPerspective";

  Object REMOTE_SONAR_JOB_FAMILY = new Object();

}
