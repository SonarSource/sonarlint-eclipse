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
package org.sonar.ide.eclipse.erlide.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.core.model.root.ErlModelManager;
import org.erlide.core.model.root.IErlModel;
import org.erlide.core.model.root.IErlProject;
import org.sonar.ide.eclipse.core.ResourceResolver;

public class ErlangResourceResolver extends ResourceResolver {

  @Override
  public String getSonarPartialKey(IResource resource) {
    IProject project = resource.getProject();
    IErlModel model = ErlModelManager.getErlangModel();
    IErlProject erlProject = model.getErlangProject(project);
    if (erlProject != null) {
      for (String src : SonarErlIdePlugin.getSourceFolders(project.getLocation().toOSString(), erlProject)) {
        IPath srcPath = new Path(getAbsolutePath(new Path(src)));
        if (srcPath.isPrefixOf(resource.getLocation())) {
          return SonarErlIdePlugin.getRelativePath(srcPath, resource.getLocation());
        }
      }
    }
    return null;
  }

  @Override
  public IResource locate(IProject project, String resourceKey) {
    IErlModel model = ErlModelManager.getErlangModel();
    IErlProject erlProject = model.getErlangProject(project);
    if (erlProject != null) {
      for (String src : SonarErlIdePlugin.getSourceFolders(project.getLocation().toOSString(), erlProject)) {
        IPath srcPath = new Path(getAbsolutePath(new Path(src)));
        IPath resourcePath = srcPath.append(resourceKey);
        if (resourcePath.toFile().exists()) {
          IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
          return root.getFileForLocation(resourcePath);
        }
      }
    }
    return null;
  }

}
