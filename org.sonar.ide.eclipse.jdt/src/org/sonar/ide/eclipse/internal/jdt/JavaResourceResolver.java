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
package org.sonar.ide.eclipse.internal.jdt;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.eclipse.core.ResourceResolver;

public class JavaResourceResolver extends ResourceResolver {

  @Override
  public String getSonarPartialKey(IResource resource) {
    IJavaElement javaElement = JavaCore.create(resource);
    if (javaElement instanceof ICompilationUnit) {
      String packageName = getPackageName(javaElement.getParent());
      String className = StringUtils.substringBeforeLast(javaElement.getElementName(), "."); //$NON-NLS-1$
      return packageName + "." + className; //$NON-NLS-1$
    } else if (javaElement instanceof IPackageFragmentRoot) {
      return "[default]"; //$NON-NLS-1$
    } else if (javaElement instanceof IPackageFragment) {
      return getPackageName(javaElement);
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
    if (StringUtils.isEmpty(packageName)) {
      packageName = "[default]"; //$NON-NLS-1$
    }
    return packageName;
  }

  @Override
  public IResource locate(IProject project, String resourceKey) {
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject != null) {
      try {
        String className = StringUtils.removeStart(resourceKey, "[default]."); //$NON-NLS-1$

        IType type = javaProject.findType(className);
        if (type == null) {
          return null;
        }
        IResource result = type.getCompilationUnit().getResource();
        return result instanceof IFile ? result : null;
      } catch (JavaModelException e) {
        return null;
      }
    }
    return null;
  }

}
