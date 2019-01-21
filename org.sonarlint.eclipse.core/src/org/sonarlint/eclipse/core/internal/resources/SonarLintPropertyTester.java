/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.resources;

import javax.annotation.Nullable;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IAdaptable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintPropertyTester extends PropertyTester {

  @Override
  public boolean test(Object receiver, String property, Object[] args, @Nullable Object expectedValue) {
    ISonarLintProject project = getProject(receiver);
    if (project == null) {
      return false;
    }

    if ("bound".equals(property)) {
      boolean isBound = SonarLintCorePlugin.loadConfig(project).isBound();
      return expectedValue == null
        ? isBound
        : (isBound == ((Boolean) expectedValue).booleanValue());
    }
    if ("autoAnalysisEnabled".equals(property)) {
      boolean isAuto = SonarLintCorePlugin.loadConfig(project).isAutoEnabled();
      return expectedValue == null
        ? isAuto
        : (isAuto == ((Boolean) expectedValue).booleanValue());
    }
    if ("open".equals(property)) {
      return expectedValue == null
        ? project.isOpen()
        : (project.isOpen() == ((Boolean) expectedValue).booleanValue());
    }
    if ("excluded".equals(property)) {
      ISonarLintFile file = getFile(receiver);
      if (file == null) {
        return true;
      }
      return FileExclusionsChecker.isPathAlreadyExcludedInProject(file);
    }
    return false;
  }

  private static ISonarLintFile getFile(Object receiver) {
    if (receiver instanceof ISonarLintFile) {
      return (ISonarLintFile) receiver;
    }
    return null;
  }

  private static ISonarLintProject getProject(Object receiver) {
    if (receiver instanceof ISonarLintProject) {
      return (ISonarLintProject) receiver;
    }
    if (receiver instanceof ISonarLintFile) {
      return ((ISonarLintFile) receiver).getProject();
    }
    if (receiver instanceof IAdaptable) {
      ISonarLintProject project = Adapters.adapt(receiver, ISonarLintProject.class);
      if (project != null) {
        return project;
      }
      ISonarLintFile file = Adapters.adapt(receiver, ISonarLintFile.class);
      if (file != null) {
        return file.getProject();
      }
    }
    return null;
  }
}
