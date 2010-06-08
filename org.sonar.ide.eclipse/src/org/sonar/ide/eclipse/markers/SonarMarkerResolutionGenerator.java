/*
 * Copyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.markers.resolvers.ISonarResolver;

/**
 * @author Jérémie Lagarde
 */
public class SonarMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  // ID from resolver extension point
  private static final String RESOLVER_ID = "org.sonar.ide.eclipse.resolver";

  public boolean hasResolutions(final IMarker marker) {
    try {
      return SonarPlugin.MARKER_ID.equals(marker.getType());
    } catch (final CoreException e) {
      return false;
    }
  }

  public IMarkerResolution[] getResolutions(final IMarker marker) {
    final List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
    final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(RESOLVER_ID);
    try {
      for (final IConfigurationElement element : config) {
        final Object resolver = element.createExecutableExtension("class");
        if (resolver instanceof ISonarResolver) {
          resolutions.add(new SonarMarkerResolution((ISonarResolver) resolver));
        }
      }
    } catch (final CoreException ex) {
      System.out.println(ex.getMessage());
    }
    return resolutions.toArray(new IMarkerResolution[resolutions.size()]);
  }

}
