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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ISonarFile;
import org.sonar.ide.eclipse.core.ISonarProject;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.core.SonarKeyUtils;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.services.Resource;

/**
 * Adapter factory for Java elements.
 */
@SuppressWarnings("rawtypes")
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {ISonarResource.class, ISonarFile.class, ISonarProject.class, Resource.class, IFile.class};

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarResource.class.equals(adapterType) || ISonarFile.class.equals(adapterType) || ISonarProject.class.equals(adapterType)) {
      return getSonarResource(adaptableObject);
    }

    if (adapterType == Resource.class) {
      if (adaptableObject instanceof IJavaProject) {
        IJavaProject javaProject = (IJavaProject) adaptableObject;
        String key = SonarUiPlugin.getSonarProject(javaProject.getProject()).getKey();
        return new Resource().setKey(key);
      }
    } else if (adapterType == IFile.class) {
      if (adaptableObject instanceof Resource) {
        Resource resource = (Resource) adaptableObject;
        String key = resource.getKey();
        String keyWithoutBranch = key.replaceFirst("([^:]*):([^:]*):([^:]*):([^:]*)", "$1:$2:$4");
        String[] parts = StringUtils.split(keyWithoutBranch, SonarKeyUtils.PROJECT_DELIMITER);
        String groupId = parts.length > 0 ? parts[0] : "";
        String artifactId = parts.length > 1 ? parts[1] : "";
        String className = StringUtils.removeStart(parts.length > 2 ? parts[2] : "", "[default]."); //$NON-NLS-1$

        IWorkspace root = ResourcesPlugin.getWorkspace();
        // TODO this is not optimal
        for (IProject project : root.getRoot().getProjects()) {
          if (project.isAccessible()) {
            ISonarProject sonarProject = SonarUiPlugin.getSonarProject(project);
            if ((sonarProject != null) && StringUtils.equals(sonarProject.getGroupId(), groupId)
              && StringUtils.equals(sonarProject.getArtifactId(), artifactId)) {
              IJavaProject javaProject = JavaCore.create(project);
              try {
                IType type = javaProject.findType(className);
                if (type == null) {
                  return null;
                }
                IResource result = type.getCompilationUnit().getResource();
                return result instanceof IFile ? result : null;
              } catch (JavaModelException e) {
                LoggerFactory.getLogger(getClass()).warn(e.getMessage(), e);
              }
            }
          }
        }
        return null;
      }
    }
    return null;
  }

  private ISonarResource getSonarResource(Object adaptableObject) {
    if (adaptableObject instanceof IJavaElement) {
      IJavaElement javaElement = (IJavaElement) adaptableObject;
      return (ISonarResource) getAdapter(javaElement.getResource(), ISonarResource.class);
    } else if (adaptableObject instanceof IProject) {
      IProject project = (IProject) adaptableObject;
      if (!isConfigured(project)) {
        return null;
      }
      return SonarUiPlugin.getSonarProject(project);
    } else if (adaptableObject instanceof IFolder) {
      IFolder folder = (IFolder) adaptableObject;
      IProject project = folder.getProject();
      if (!isConfigured(project)) {
        return null;
      }
      ISonarProject sonarProject = SonarUiPlugin.getSonarProject(folder.getProject());
      String packageName = getPackageName(JavaCore.create(folder));
      if (packageName != null) {
        return SonarCorePlugin.createSonarResource(folder, SonarKeyUtils.packageKey(sonarProject, packageName), packageName);
      }
    } else if (adaptableObject instanceof IFile) {
      IFile file = (IFile) adaptableObject;
      IProject project = file.getProject();
      if (!isConfigured(project)) {
        return null;
      }
      ISonarProject sonarProject = SonarUiPlugin.getSonarProject(file.getProject());
      IJavaElement javaElement = JavaCore.create(file);
      if (javaElement instanceof ICompilationUnit) {
        String packageName = getPackageName(javaElement.getParent());
        String className = StringUtils.substringBeforeLast(javaElement.getElementName(), "."); //$NON-NLS-1$
        return SonarCorePlugin.createSonarFile(file, SonarKeyUtils.classKey(sonarProject, packageName, className), className);
      }
    }
    return null;
  }

  private boolean isConfigured(IProject project) {
    return project.isAccessible() && SonarUiPlugin.hasSonarNature(project) && SonarUiPlugin.hasJavaNature(project);
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
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
