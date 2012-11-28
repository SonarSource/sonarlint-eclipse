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

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ResourceResolver;

public class CResourceResolver extends ResourceResolver {

  @Override
  public String getSonarPartialKey(IResource resource) {
    ICElement cElement = CoreModel.getDefault().create(resource);
    if (cElement != null) {
      ICContainer sourceRoot = SonarCdtPlugin.getSourceFolder(cElement);
      if (sourceRoot != null) {
        return SonarCdtPlugin.getRelativePath(sourceRoot.getPath(), cElement.getPath());
      }
    }
    return null;
  }

  @Override
  public IResource locate(IProject project, String resourceKey) {
    ICProject cProject = CoreModel.getDefault().create(project);
    if (cProject != null) {
      try {
        String relativeFilePath = resourceKey;

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
    return null;
  }

}
