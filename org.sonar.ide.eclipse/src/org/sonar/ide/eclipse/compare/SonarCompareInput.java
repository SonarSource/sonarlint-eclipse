/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class SonarCompareInput extends CompareEditorInput {

  protected final IResource resource;
  protected final String sourceCode;

  public SonarCompareInput(IResource resource, String sourceCode) {
    super(new CompareConfiguration());
    this.resource = resource;
    this.sourceCode = sourceCode;
  }

  @Override
  protected Object prepareInput(final IProgressMonitor monitor) {
    final ITypedElement left = new ResourceNode(resource);
    final ITypedElement right = new SonarCompareNode(resource.getName(), sourceCode);
    final CompareConfiguration config = getCompareConfiguration();
    config.setLeftLabel(left.getName());
    config.setLeftEditable(true);
    config.setRightLabel(right.getName() + " (sonar server)");
    config.setRightEditable(false);
    config.setRightImage(SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONAR16).createImage());
    return new DiffNode(left, right);
  }
}
