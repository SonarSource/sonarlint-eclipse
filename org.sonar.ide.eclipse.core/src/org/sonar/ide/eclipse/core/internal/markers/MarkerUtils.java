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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.wsclient.services.Violation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class MarkerUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MarkerUtils.class);

  private static int markerSeverity = IMarker.SEVERITY_WARNING;

  private MarkerUtils() {
  }

  public static void createMarkersForJSONViolations(IResource resource, JSONArray violations) {
    for (Object violationObj : violations) {
      JSONObject jsonViolation = (JSONObject) violationObj;
      Violation violation = new Violation();
      Long line = (Long) jsonViolation.get("line");//$NON-NLS-1$
      violation.setLine(line != null ? line.intValue() : null);
      violation.setMessage(ObjectUtils.toString(jsonViolation.get("message")));//$NON-NLS-1$
      violation.setSeverity(ObjectUtils.toString(jsonViolation.get("severity")));//$NON-NLS-1$
      violation.setRuleKey(ObjectUtils.toString(jsonViolation.get("rule_key")));//$NON-NLS-1$
      violation.setRuleName(ObjectUtils.toString(jsonViolation.get("rule_name"))); //$NON-NLS-1$
      violation.setSwitchedOff("true".equals(jsonViolation.get("switched_off"))); //$NON-NLS-1$
      try {
        createMarkerForWSViolation(resource, violation);
      } catch (CoreException e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  public static void createMarkerForWSViolation(final IResource resource, final Violation violation) throws CoreException {
    if (violation.isSwitchedOff()) {
      // SONARIDE-281
      return;
    }
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    Integer line = violation.getLine();
    markerAttributes.put(IMarker.PRIORITY, getPriority(violation));
    markerAttributes.put(IMarker.SEVERITY, markerSeverity);
    // File level violation (line == null) are displayed on line 1
    markerAttributes.put(IMarker.LINE_NUMBER, line != null ? line : 1);
    markerAttributes.put(IMarker.MESSAGE, getMessage(violation));

    String source = "";
    if (resource instanceof IFile) {
      IFile file = (IFile) resource;
      InputStream inputStream = file.getContents();
      try {
        source = IOUtils.toString(inputStream, file.getCharset());
      } catch (IOException e) {
        LOG.error("Unable to read source of " + resource.getLocation().toOSString(), e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }

    if (line != null) {
      addLine(markerAttributes, line, source);
    }
    final Map<String, Object> extraInfos = getExtraInfos(violation);
    if (extraInfos != null) {
      for (Map.Entry<String, Object> entry : extraInfos.entrySet()) {
        markerAttributes.put(entry.getKey(), entry.getValue());
      }
    }
    final IMarker marker = resource.createMarker(SonarCorePlugin.MARKER_ID);
    marker.setAttributes(markerAttributes);
  }

  private static String getMessage(final Violation violation) {
    return violation.getRuleName() + " : " + violation.getMessage();
  }

  /**
   * @return Priority marker attribute. A number from the set of high, normal and low priorities defined by the platform.
   *
   * @see IMarker.PRIORITY_HIGH
   * @see IMarker.PRIORITY_NORMAL
   * @see IMarker.PRIORITY_LOW
   */
  private static Integer getPriority(final Violation violation) {
    final int result;
    if ("blocker".equalsIgnoreCase(violation.getSeverity())) {
      result = Integer.valueOf(IMarker.PRIORITY_HIGH);
    }
    else if ("critical".equalsIgnoreCase(violation.getSeverity())) {
      result = Integer.valueOf(IMarker.PRIORITY_HIGH);
    }
    else if ("major".equalsIgnoreCase(violation.getSeverity())) {
      result = Integer.valueOf(IMarker.PRIORITY_NORMAL);
    }
    else if ("minor".equalsIgnoreCase(violation.getSeverity())) {
      result = Integer.valueOf(IMarker.PRIORITY_LOW);
    }
    else if ("info".equalsIgnoreCase(violation.getSeverity())) {
      result = Integer.valueOf(IMarker.PRIORITY_LOW);
    }
    else {
      result = Integer.valueOf(IMarker.PRIORITY_LOW);
    }
    return result;
  }

  private static Map<String, Object> getExtraInfos(final Violation violation) {
    final Map<String, Object> extraInfos = new HashMap<String, Object>();
    extraInfos.put("rulekey", violation.getRuleKey());
    extraInfos.put("rulename", violation.getRuleName());
    extraInfos.put("rulepriority", violation.getSeverity());
    if (violation.getId() != null) {
      extraInfos.put("violationId", Long.toString(violation.getId()));
    }
    if (violation.getReview() != null) {
      extraInfos.put("reviewId", Long.toString(violation.getReview().getId()));
    }
    return extraInfos;
  }

  private static void addLine(final Map<String, Object> markerAttributes, final long line, final String text) {
    int start = 0;
    for (int i = 1; i < line; i++) {
      start = StringUtils.indexOf(text, '\n', start) + 1;
    }
    final int end = StringUtils.indexOf(text, '\n', start);
    markerAttributes.put(IMarker.CHAR_START, start);
    markerAttributes.put(IMarker.CHAR_END, end);
  }

  public static void deleteViolationsMarkers(IResource resource) {
    try {
      resource.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  public static void updateAllSonarMarkerSeverity(int newSeverity) throws CoreException {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        for (IMarker marker : project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE)) {
          marker.setAttribute(IMarker.SEVERITY, newSeverity);
        }
      }
    }
  }

  public static void setMarkerSeverity(int severity) {
    markerSeverity = severity;
  }

}
