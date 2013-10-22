/*
 * SonarQube Eclipse
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
package org.sonar.ide.eclipse.core.internal.remote;

import org.sonar.ide.eclipse.common.issues.ISonarIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
public final class IssuesUtils {

  public static List<ISonarIssue> convertLines(List<ISonarIssue> issues, SourceCodeDiff diff) {
    List<ISonarIssue> result = new ArrayList<ISonarIssue>();

    for (ISonarIssue issue : issues) {
      Integer originalLine = issue.line();
      if (originalLine == null) {
        result.add(issue);
        continue;
      }

      int newLine = diff.newLine(originalLine);
      // skip issue, which doesn't match any line
      if (newLine != -1) {
        result.add(new SonarIssueWithModifiedLine(issue, newLine));
      }
    }
    return result;
  }

  private static class SonarIssueWithModifiedLine implements ISonarIssue {

    private final ISonarIssue original;
    private final int newLine;

    public SonarIssueWithModifiedLine(final ISonarIssue original, int newLine) {
      this.original = original;
      this.newLine = newLine;
    }

    @Override
    public String key() {
      return original.key();
    }

    @Override
    public String resourceKey() {
      return original.resourceKey();
    }

    @Override
    public boolean resolved() {
      return original.resolved();
    }

    @Override
    public Integer line() {
      return newLine;
    }

    @Override
    public String severity() {
      return original.severity();
    }

    @Override
    public String message() {
      return original.message();
    }

    @Override
    public String ruleKey() {
      return original.ruleKey();
    }

    @Override
    public String ruleName() {
      return original.ruleName();
    }

    @Override
    public String assignee() {
      return original.assignee();
    }

  }

  /**
   * Hide utility-class constructor.
   */
  private IssuesUtils() {
  }
}
