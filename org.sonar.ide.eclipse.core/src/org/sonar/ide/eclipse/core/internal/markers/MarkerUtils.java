/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import java.util.Map;

public final class MarkerUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MarkerUtils.class);

  static int markerSeverity = IMarker.SEVERITY_WARNING;
  static int markerSeverityForNewViolations = IMarker.SEVERITY_ERROR;

  public static final String SONAR_MARKER_RULE_KEY_ATTR = "rulekey";
  public static final String SONAR_MARKER_RULE_NAME_ATTR = "rulename";
  public static final String SONAR_MARKER_RULE_PRIORITY_ATTR = "rulepriority";
  public static final String SONAR_MARKER_ISSUE_ID_ATTR = "issueId";
  public static final String SONAR_MARKER_IS_NEW_ATTR = "is_new";

  private MarkerUtils() {
  }

  public static void createMarkersForJSONIssues(Map<String, IResource> resourcesByKey, Map<String, String> ruleByKey, JSONArray issues) {
    for (Object issueObj : issues) {
      JSONObject jsonIssue = (JSONObject) issueObj;
      String componentKey = ObjectUtils.toString(jsonIssue.get("component"));
      if (resourcesByKey.containsKey(componentKey)) {
        boolean isNew = Boolean.TRUE.equals(jsonIssue.get("isNew")); //$NON-NLS-1$
        try {
          SonarMarker.create(resourcesByKey.get(componentKey), isNew, new SonarIssueFromJsonReport(jsonIssue, ruleByKey));
        } catch (CoreException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
  }

  private static class SonarIssueFromJsonReport implements ISonarIssue {

    private JSONObject jsonIssue;
    private Map<String, String> ruleByKey;

    public SonarIssueFromJsonReport(JSONObject jsonIssue, Map<String, String> ruleByKey) {
      this.jsonIssue = jsonIssue;
      this.ruleByKey = ruleByKey;
    }

    @Override
    public String key() {
      return ObjectUtils.toString(jsonIssue.get("key")); //$NON-NLS-1$
    }

    @Override
    public String resourceKey() {
      return ObjectUtils.toString(jsonIssue.get("component")); //$NON-NLS-1$
    }

    @Override
    public boolean resolved() {
      return StringUtils.isNotBlank(ObjectUtils.toString(jsonIssue.get("resolution"))); //$NON-NLS-1$
    }

    @Override
    public Integer line() {
      Long line = (Long) jsonIssue.get("line");//$NON-NLS-1$
      return line != null ? line.intValue() : null;
    }

    @Override
    public String severity() {
      return ObjectUtils.toString(jsonIssue.get("severity"));//$NON-NLS-1$
    }

    @Override
    public String description() {
      return ObjectUtils.toString(jsonIssue.get("description"));//$NON-NLS-1$
    }

    @Override
    public String ruleKey() {
      return ObjectUtils.toString(jsonIssue.get("rule"));//$NON-NLS-1$
    }

    @Override
    public String ruleName() {
      return ObjectUtils.toString(ruleByKey.get(ruleKey()));//$NON-NLS-1$
    }

  }

  public static void deleteIssuesMarkers(IResource resource) {
    try {
      resource.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  public static void updateAllSonarMarkerSeverity() throws CoreException {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        for (IMarker marker : project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE)) {
          boolean isNew = marker.getAttribute(SONAR_MARKER_IS_NEW_ATTR, false);
          marker.setAttribute(IMarker.SEVERITY, isNew ? markerSeverityForNewViolations : markerSeverity);
        }
      }
    }
  }

  public static void setMarkerSeverity(int severity) {
    markerSeverity = severity;
  }

  public static void setMarkerSeverityForNewIssues(int severity) {
    markerSeverityForNewViolations = severity;
  }

}
