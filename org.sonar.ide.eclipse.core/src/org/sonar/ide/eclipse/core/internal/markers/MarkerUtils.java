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
package org.sonar.ide.eclipse.core.internal.markers;

import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class MarkerUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MarkerUtils.class);

  private MarkerUtils() {
  }

  public static void createMarkersForViolations(IResource resource, JSONArray violations) {
    for (Object violationObj : violations) {
      JSONObject violation = (JSONObject) violationObj;
      final Map<String, Object> markerAttributes = Maps.newHashMap();
      Long line = (Long) violation.get("line");
      markerAttributes.put(IMarker.LINE_NUMBER, line != null ? line.intValue() : 1); // SONARIDE-64
      markerAttributes.put(IMarker.MESSAGE, ObjectUtils.toString(violation.get("message")));
      markerAttributes.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
      markerAttributes.put(IMarker.PRIORITY, IMarker.PRIORITY_LOW);

      markerAttributes.put("rulekey", ObjectUtils.toString(violation.get("rule_key"))); //$NON-NLS-1$
      markerAttributes.put("rulename", ObjectUtils.toString(violation.get("rule_name"))); //$NON-NLS-1$
      markerAttributes.put("rulepriority", ObjectUtils.toString(violation.get("severity"))); //$NON-NLS-1$

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
