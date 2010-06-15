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

package org.sonar.ide.eclipse.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.markers.resolvers.ISonarResolver;

/**
 * @author Jérémie Lagarde
 */
public class SonarMarkerResolution implements IMarkerResolution2 {

  ISonarResolver resolver;

  SonarMarkerResolution(final ISonarResolver sonarResolver) {
    resolver = sonarResolver;
  }

  public String getDescription() {
    return resolver.getDescription();
  }

  public Image getImage() {
    return SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONAR16).createImage();
  }

  public String getLabel() {
    return resolver.getLabel();
  }

  public void run(final IMarker marker) {
    final IResource resource = marker.getResource();
    if (resource instanceof IFile && resource.isAccessible()) {
      final IJavaElement element = JavaCore.create((IFile) resource);
      if (element instanceof ICompilationUnit) {
        if (resolver.resolve(marker, (ICompilationUnit) element)) {
          try {
            marker.delete();
          } catch (final CoreException e) {
            SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
          }
        }
      }
    }
  }
}
