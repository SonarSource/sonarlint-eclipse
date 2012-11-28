/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.core;

import org.apache.commons.lang.StringUtils;
import org.sonar.ide.eclipse.core.ISonarProject;

public final class SonarKeyUtils {
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

  private SonarKeyUtils() {
  }

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
  public static String packageKey(ISonarProject project, String packageName) {
    return project.getKey() + PROJECT_DELIMITER + StringUtils.defaultIfEmpty(packageName, DEFAULT_PACKAGE_NAME);
  }

  /**
   * Examples:
   * <ul>
   * <li>myproject:ui/foo.c</li>
   * </ul>
   *
   * @return key for non Java resources
   */
  public static String resourceKey(ISonarProject project, String resourcePath) {
    return project.getKey() + PROJECT_DELIMITER + resourcePath;
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
  public static String classKey(ISonarProject project, String packageName, String fileName) {
    return packageKey(project, packageName) + PACKAGE_DELIMITER + fileName;
  }
}
