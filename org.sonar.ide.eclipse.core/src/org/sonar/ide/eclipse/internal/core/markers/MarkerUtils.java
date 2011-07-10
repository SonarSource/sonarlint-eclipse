/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.core.markers;

import com.google.common.collect.Maps;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.ide.eclipse.core.SonarCorePlugin;

import java.util.Collection;
import java.util.Map;

public final class MarkerUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MarkerUtils.class);

  private MarkerUtils() {
  }

  public static void createMarkersForViolations(IResource resource, Collection<Violation> violations) {
    for (Violation violation : violations) {
      final Map<String, Object> markerAttributes = Maps.newHashMap();
      markerAttributes.put(IMarker.LINE_NUMBER, violation.getLineId());
      markerAttributes.put(IMarker.MESSAGE, violation.getMessage());
      markerAttributes.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
      markerAttributes.put(IMarker.PRIORITY, IMarker.PRIORITY_LOW);

      Rule rule = violation.getRule();
      markerAttributes.put("rulekey", rule.getKey()); //$NON-NLS-1$
      markerAttributes.put("rulename", rule.getName()); //$NON-NLS-1$
      // Don't use rule.getSeverity() here - see SONARIDE-218
      markerAttributes.put("rulepriority", violation.getSeverity().toString()); //$NON-NLS-1$

      try {
        IMarker marker = resource.createMarker(SonarCorePlugin.MARKER_ID);
        marker.setAttributes(markerAttributes);
      } catch (CoreException e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  public static void deleteViolationsMarkers(IResource resource) {
    try {
      resource.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_ZERO);
    } catch (CoreException e) {
      LOG.error(e.getMessage(), e);
    }
  }

}
