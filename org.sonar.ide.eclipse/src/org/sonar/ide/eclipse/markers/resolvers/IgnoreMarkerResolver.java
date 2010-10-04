/*
 * Copyright (C) 2010 Evgeny Mandrikov, Lagarde jérémie
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

package org.sonar.ide.eclipse.markers.resolvers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.core.ISonarConstants;

import java.text.MessageFormat;

/**
 * @author Jérémie Lagarde
 */
public class IgnoreMarkerResolver implements ISonarResolver {
  private String label;
  private String description;

  public boolean canResolve(final IMarker marker) {
    try {
      if (ISonarConstants.MARKER_ID.equals(marker.getType())) {
        final Object ruleName = marker.getAttribute("rulename"); //$NON-NLS-1$
        label = MessageFormat.format(Messages.getString("resolver.ignoremarker.label"), ruleName); //$NON-NLS-1$
        description = MessageFormat.format(Messages.getString("resolver.ignoremarker.description"), ruleName); //$NON-NLS-1$
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
