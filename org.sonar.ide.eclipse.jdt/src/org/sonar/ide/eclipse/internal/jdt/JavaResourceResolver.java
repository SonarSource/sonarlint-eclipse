/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.jdt;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.sonar.ide.eclipse.core.AbstractResourceResolver;

public class JavaResourceResolver extends AbstractResourceResolver {

  @Override
  public String resolve(IResource resource, IProgressMonitor monitor) {
    IJavaElement javaElement = JavaCore.create(resource);
    if (javaElement instanceof ICompilationUnit) {
      String packageName = getPackageName(javaElement.getParent());
      String className = StringUtils.substringBeforeLast(javaElement.getElementName(), "."); //$NON-NLS-1$

      if (StringUtils.isEmpty(packageName)) {
        packageName = "[default]"; //$NON-NLS-1$
      }
      return packageName + "." + className; //$NON-NLS-1$
    }
    return null;
  }

  private String getPackageName(IJavaElement javaElement) {
    String packageName = null;
    if (javaElement instanceof IPackageFragmentRoot) {
      packageName = ""; //$NON-NLS-1$
    } else if (javaElement instanceof IPackageFragment) {
      IPackageFragment packageFragment = (IPackageFragment) javaElement;
      packageName = packageFragment.getElementName();
    }
    return packageName;
  }

}
