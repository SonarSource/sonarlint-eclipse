/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

public class SonarLintPropertyTester extends PropertyTester {

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IProject project = null;
    if (receiver instanceof IProject) {
      project = (IProject) receiver;
    } else if (receiver instanceof IResource) {
      project = ((IResource) receiver).getProject();
    } else if (receiver instanceof IAdaptable) {
      project = (IProject) ((IAdaptable) receiver).getAdapter(IProject.class);
      if (project == null) {
        IResource res = (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
        if (res != null) {
          project = res.getProject();
        }
      }
    }
    if (project != null && "isBound".equals(property)) {
      SonarLintProject p = SonarLintProject.getInstance(project);
      return expectedValue.equals(p.isBound());
    }
    if (project != null && "isAutoAnalysis".equals(property)) {
      SonarLintProject p = SonarLintProject.getInstance(project);
      return expectedValue.equals(p.isAutoEnabled());
    }
    return false;
  }

}
