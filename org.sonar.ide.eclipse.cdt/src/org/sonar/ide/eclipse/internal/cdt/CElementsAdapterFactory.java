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
package org.sonar.ide.eclipse.internal.cdt;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
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
public class CElementsAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {ISonarResource.class, ISonarFile.class, ISonarProject.class, Resource.class, IFile.class};

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarResource.class.equals(adapterType) || ISonarFile.class.equals(adapterType) || ISonarProject.class.equals(adapterType)) {
      return getSonarResource(adaptableObject);
    }

    if (adapterType == Resource.class) {
      if (adaptableObject instanceof ICProject) {
        ICProject cProject = (ICProject) adaptableObject;
        String key = SonarUiPlugin.getSonarProject(cProject.getProject()).getKey();
        return new Resource().setKey(key);
      }
    } else if (adapterType == IFile.class) {
      if (adaptableObject instanceof Resource) {
        Resource resource = (Resource) adaptableObject;

        IWorkspace root = ResourcesPlugin.getWorkspace();
        // TODO this is not optimal
        for (IProject project : root.getRoot().getProjects()) {
          if (project.isAccessible()) {
            ISonarProject sonarProject = SonarUiPlugin.getSonarProject(project);
            if ((sonarProject != null) && resource.getKey().startsWith(sonarProject.getKey())) {
              ICProject cProject = CoreModel.getDefault().create(project);
              try {
                String resourceKeyMinusProjectKey = resource.getKey().substring(
                    sonarProject.getKey().length() + 1); // +1 because ":"
                String[] parts = StringUtils.split(resourceKeyMinusProjectKey, SonarKeyUtils.PROJECT_DELIMITER);
                String relativeFilePath = parts[0];

                // Now we have to iterate over source folders to find the location of the file
                for (ISourceRoot sourceRoot : cProject.getAllSourceRoots()) {
                  IPath potentialPath = sourceRoot.getPath().append(relativeFilePath);
                  if (potentialPath.toFile().exists()) {
                    return CoreModel.getDefault().create(potentialPath).getResource();
                  }
                }
                return null;
              } catch (CModelException e) {
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
    if (adaptableObject instanceof ICElement) {
      ICElement cElement = (ICElement) adaptableObject;
      return (ISonarResource) getAdapter(cElement.getResource(), ISonarResource.class);
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
      ICContainer cFolder = CoreModel.getDefault().create(folder);
      if (cFolder != null) {
        ICContainer sourceRoot = SonarCdtPlugin.getSourceFolder(cFolder);
        if (sourceRoot != null) {
          String packageName = SonarCdtPlugin.getRelativePath(sourceRoot.getPath(), cFolder.getPath());
          return SonarCorePlugin.createSonarResource(folder, SonarKeyUtils.resourceKey(sonarProject, packageName), packageName);
        }
      }
    } else if (adaptableObject instanceof IFile) {
      IFile file = (IFile) adaptableObject;
      IProject project = file.getProject();
      if (!isConfigured(project)) {
        return null;
      }
      ISonarProject sonarProject = SonarUiPlugin.getSonarProject(file.getProject());
      ICElement cElement = CoreModel.getDefault().create(file);
      if (cElement != null) {
        ICContainer sourceRoot = SonarCdtPlugin.getSourceFolder(cElement);
        if (sourceRoot != null) {
          String packageName = SonarCdtPlugin.getRelativePath(sourceRoot.getPath(), cElement.getPath());
          return SonarCorePlugin.createSonarFile(file, SonarKeyUtils.resourceKey(sonarProject, packageName), cElement.getElementName());
        }
      }
    }
    return null;
  }

  private boolean isConfigured(IProject project) {
    return project.isAccessible() && SonarUiPlugin.hasSonarNature(project) && CoreModel.hasCNature(project);
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
