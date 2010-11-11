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

package org.sonar.ide.eclipse.core;

import org.apache.commons.lang.StringUtils;

public class SonarKeyUtils {

  public static final char PROJECT_DELIMITER = ':';

  public static final char PACKAGE_DELIMITER = '.';

  public static final char PATH_DELIMITER = '/';

  /**
   * Default package name for Java classes without package definition.
   */
  public static final String DEFAULT_PACKAGE_NAME = "[default]";

  /**
   * Default directory name for files in root directory.
   */
  public static final String DEFAULT_DIRECTORY_NAME = "[root]";

  /**
   * Examples:
   * <ul>
   * <li>org.example:myproject</li>
   * <li>org.example:myproject:branch-1.0</li>
   * </ul>
   * 
   * @return key for project
   */
  public static String projectKey(String groupId, String artifactId, String branch) {
    StringBuilder sb = new StringBuilder().append(groupId).append(PROJECT_DELIMITER).append(artifactId);
    if (StringUtils.isNotBlank(branch)) {
      sb.append(PROJECT_DELIMITER).append(branch);
    }
    return sb.toString();
  }

  /**
   * Examples:
   * <ul>
   * <li>org.example:myproject:[default]</li>
   * <li>org.example:myproject:org.example.mypackage</li>
   * </ul>
   * 
   * @return key for Java package
   */
  public static String packageKey(String projectKey, String packageName) {
    if (StringUtils.isBlank(packageName)) {
      return projectKey + PROJECT_DELIMITER + DEFAULT_PACKAGE_NAME;
    }
    return projectKey + PROJECT_DELIMITER + packageName;
  }

  /**
   * Examples:
   * <ul>
   * <li>org.example:myproject:[default].ClassOnDefaultPackage</li>
   * <li>org.example:myproject:org.example.mypackage.ClassOne</li>
   * </ul>
   * 
   * @return key for Java file
   */
  public static String classKey(String projectKey, String packageName, String fileName) {
    return packageKey(projectKey, packageName) + PACKAGE_DELIMITER + fileName;
  }

  private SonarKeyUtils() {
  }

}
