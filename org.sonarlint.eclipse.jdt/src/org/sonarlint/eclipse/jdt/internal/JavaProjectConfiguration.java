/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.jdt.internal;

import java.util.LinkedHashSet;
import java.util.Set;

public class JavaProjectConfiguration {

  private final Set<Object> dependentProjects = new LinkedHashSet<>();
  private final Set<Object> testDependentProjects = new LinkedHashSet<>();
  private final Set<String> libraries = new LinkedHashSet<>();
  private final Set<String> testLibraries = new LinkedHashSet<>();
  private final Set<String> binaries = new LinkedHashSet<>();
  private final Set<String> testBinaries = new LinkedHashSet<>();

  public Set<Object> dependentProjects() {
    return dependentProjects;
  }

  public Set<Object> testDependentProjects() {
    return testDependentProjects;
  }

  public Set<String> libraries() {
    return libraries;
  }

  public Set<String> testLibraries() {
    return testLibraries;
  }

  public Set<String> binaries() {
    return binaries;
  }

  public Set<String> testBinaries() {
    return testBinaries;
  }

}
