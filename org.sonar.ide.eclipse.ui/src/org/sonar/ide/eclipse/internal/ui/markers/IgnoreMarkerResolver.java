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
package org.sonar.ide.eclipse.internal.ui.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.ui.ISonarResolver;

import java.text.MessageFormat;

/**
 * @author Jérémie Lagarde
 */
public class IgnoreMarkerResolver implements ISonarResolver {
  private String label;
  private String description;

  public boolean canResolve(final IMarker marker) {
    try {
      if (SonarCorePlugin.MARKER_ID.equals(marker.getType())) {
        final Object ruleName = marker.getAttribute("rulename"); //$NON-NLS-1$
        label = MessageFormat.format(Messages.IgnoreMarkerResolver_label, ruleName);
        description = MessageFormat.format(Messages.IgnoreMarkerResolver_description, ruleName);
        return true;
      }
    } catch (final CoreException e) {
      return false;
    }
    return false;
  }

  public String getDescription() {
    return description;
  }

  public String getLabel() {
    return label;
  }

  public boolean resolve(final IMarker marker, final IFile cu) {
    return true;
  }
}
