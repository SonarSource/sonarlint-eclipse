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

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.ResourceResolver;

public class CResourceResolver extends ResourceResolver {

  @Override
  public String resolve(IResource resource, IProgressMonitor monitor) {
    ICElement cElement = CoreModel.getDefault().create(resource);
    if (cElement != null) {
      ICContainer sourceRoot = SonarCdtPlugin.getSourceFolder(cElement);
      if (sourceRoot != null) {
        return SonarCdtPlugin.getRelativePath(sourceRoot.getPath(), cElement.getPath());
      }
    }
    return null;
  }

}
