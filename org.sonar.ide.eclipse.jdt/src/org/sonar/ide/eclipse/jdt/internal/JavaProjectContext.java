/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.jdt.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

public class JavaProjectContext {

  private final Set<String> sourceDirs = new HashSet<String>();
  private final Set<String> testDirs = new HashSet<String>();
  private final Set<String> libraries = new HashSet<String>();
  private final Set<String> binaries = new HashSet<String>();
  private final Set<IJavaProject> projects = new HashSet<IJavaProject>();

  public Set<IJavaProject> getProjects() {
    return projects;
  }
  public Set<String> getSourceDirs() {
    return sourceDirs;
  }
  public Set<String> getTestDirs() {
    return testDirs;
  }
  public Set<String> getLibraries() {
    return libraries;
  }
  public Set<String> getBinaries() {
    return binaries;
  }
}
