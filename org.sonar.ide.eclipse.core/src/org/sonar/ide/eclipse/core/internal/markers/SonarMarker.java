/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2014 SonarSource
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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class SonarMarker {

  private final IResource resource;
  private final IMarker marker;
  private boolean isNew;

  private SonarMarker(final IResource resource, final boolean isNew) throws CoreException {
    this.resource = resource;
    this.isNew = isNew;
    this.marker = resource.createMarker(isNew ? SonarCorePlugin.NEW_ISSUE_MARKER_ID : SonarCorePlugin.MARKER_ID);
  }

  public static void create(final IResource resource, final boolean isNew, final ISonarIssue issue) throws CoreException {
    if (issue.resolved()) {
      // Don't display resolved issues
      return;
    }
    new SonarMarker(resource, isNew).from(issue);
  }

  private SonarMarker from(final ISonarIssue issue) throws CoreException {
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    Integer line = issue.line();
    markerAttributes.put(IMarker.PRIORITY, getPriority(issue.severity()));
    markerAttributes.put(IMarker.SEVERITY, isNew ? MarkerUtils.markerSeverityForNewIssues : MarkerUtils.markerSeverity);
    // File level issues (line == null) are displayed on line 1
    markerAttributes.put(IMarker.LINE_NUMBER, line != null ? line : 1);
    markerAttributes.put(IMarker.MESSAGE, getMessage(issue));
    markerAttributes.put(MarkerUtils.SONAR_MARKER_IS_NEW_ATTR, isNew);

    if (line != null) {
      addLine(markerAttributes, line, resource);
    }
    markerAttributes.put(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, issue.ruleKey());
    markerAttributes.put(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, issue.ruleName());
    markerAttributes.put(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, issue.severity());
    if (issue.key() != null) {
      markerAttributes.put(MarkerUtils.SONAR_MARKER_ISSUE_ID_ATTR, issue.key());
    }
    if (issue.assigneeLogin() != null) {
      markerAttributes.put(MarkerUtils.SONAR_MARKER_ASSIGNEE, issue.assigneeLogin());
    }
    if (issue.assigneeName() != null) {
      markerAttributes.put(MarkerUtils.SONAR_MARKER_ASSIGNEE_NAME, issue.assigneeName());
    }

    marker.setAttributes(markerAttributes);
    return this;
  }

  private static String getMessage(final ISonarIssue issue) {
    return issue.ruleName() + " : " + issue.message();
  }

  /**
   * @return Priority marker attribute. A number from the set of high, normal and low priorities defined by the platform.
   *
   * @see IMarker.PRIORITY_HIGH
   * @see IMarker.PRIORITY_NORMAL
   * @see IMarker.PRIORITY_LOW
   */
  private static Integer getPriority(final String severity) {
    int result = IMarker.PRIORITY_LOW;
    if ("blocker".equalsIgnoreCase(severity) || "critical".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_HIGH;
    } else if ("major".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_NORMAL;
    } else if ("minor".equalsIgnoreCase(severity) || "info".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_LOW;
    }
    return result;
  }

  @VisibleForTesting
  public static void addLine(final Map<String, Object> markerAttributes, final long line, final IResource resource) {
    if (resource instanceof IFile) {
      IFile file = (IFile) resource;
      InputStream is = null;
      LineAndCharCountReader lnr = null;
      try {
        is = file.getContents();
        lnr = new LineAndCharCountReader(new InputStreamReader(is, file.getCharset()));
        while (lnr.read() != -1) {
          if (lnr.getLineNumber() == line) {
            markerAttributes.put(IMarker.CHAR_START, lnr.getCharIndex());
            String s = lnr.readLine();
            if (s != null) {
              markerAttributes.put(IMarker.CHAR_END, lnr.getCharIndex() + s.length());
            }
            return;
          }
        }
      } catch (Exception e) {
        SonarCorePlugin.getDefault().error("Unable to compute position of SonarQube marker on resource " + resource.getName() + ": " + e.getMessage());
      } finally {
        IOUtils.closeQuietly(lnr);
      }
    }
  }

  /**
   * This class is used to count lines in a stream and keep char index. Contrary to {@link LineNumberReader} it preserve
   * \r\n as 2 characters.
   */
  private static class LineAndCharCountReader extends BufferedReader {

    /** The current line number */
    private int lineNumber = 1;

    private int charIndex = 0;

    /** If the previous character was a cariage return */
    private boolean cr = false;

    public LineAndCharCountReader(Reader in) {
      super(in);
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public int getCharIndex() {
      return charIndex;
    }

    @Override
    public int read() throws IOException {
      int val = super.read();
      if (val == -1) {
        if (cr) {
          lineNumber++;
          cr = false;
        }
      } else {
        charIndex++;
        if (val == '\r') {
          if (cr) {
            lineNumber++;
          } else {
            cr = true;
          }
        } else if (val == '\n' || cr) {
          lineNumber++;
          cr = false;
        }
      }

      return val;
    }

  }
}
