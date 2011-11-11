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
package org.sonar.ide.shared.violations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.ide.api.SourceCodeDiff;
import org.sonar.wsclient.services.Violation;

/**
 * @author Evgeny Mandrikov
 */
public final class ViolationUtils {
  public static final String PRIORITY_BLOCKER = "blocker";
  public static final String PRIORITY_CRITICAL = "critical";
  public static final String PRIORITY_MAJOR = "major";
  public static final String PRIORITY_MINOR = "minor";
  public static final String PRIORITY_INFO = "info";

  private static final String[] PRIORITIES = new String[] {
    PRIORITY_BLOCKER,
    PRIORITY_CRITICAL,
    PRIORITY_MAJOR,
    PRIORITY_MINOR,
    PRIORITY_INFO
  };

  /**
   * Sorts violations by priority in descending order.
   * 
   * @param violations list of violations to sort
   * @return sorted list of violations
   */
  public static List<Violation> sortByPriority(List<Violation> violations) {
    Collections.sort(violations, new PriorityComparator());
    return violations;
  }

  public static Map<Integer, List<Violation>> splitByLines(Collection<Violation> violations) {
    Map<Integer, List<Violation>> violationsByLine = new HashMap<Integer, List<Violation>>();
    for (Violation violation : violations) {
      final List<Violation> collection;
      if (violationsByLine.containsKey(violation.getLine())) {
        collection = violationsByLine.get(violation.getLine());
      } else {
        collection = new ArrayList<Violation>();
        violationsByLine.put(violation.getLine(), collection);
      }
      collection.add(violation);
    }
    return violationsByLine;
  }

  /**
   * Converts priority from string to integer.
   * 
   * @param priority priority to convert
   * @return converted priority
   */
  public static int convertPriority(String priority) {
    for (int i = 0; i < PRIORITIES.length; i++) {
      if (PRIORITIES[i].equalsIgnoreCase(priority)) {
        return i;
      }
    }
    return 4;
  }

  public static String getDescription(Violation violation) {
    return violation.getRuleName() + " : " + violation.getMessage();
  }

  public static List<Violation> convertLines(Collection<Violation> violations, SourceCodeDiff diff) {
    List<Violation> result = new ArrayList<Violation>();

    for (Violation violation : violations) {
      Integer originalLine = violation.getLine();
      if (originalLine == null || originalLine == 0) {
        // skip violation on whole file
        // TODO Godin: we can show them on first line
        continue;
      }

      int newLine = diff.newLine(originalLine);
      // skip violation, which doesn't match any line
      if (newLine != -1) {
        violation.setLine(newLine);
        result.add(violation);
      }
    }
    return result;
  }

  static class PriorityComparator implements Comparator<Violation> {
    public int compare(Violation o1, Violation o2) {
      int p1 = convertPriority(o1.getSeverity());
      int p2 = convertPriority(o2.getSeverity());
      return p1 - p2;
    }
  }

  /**
   * @param violations collection to convert to string
   * @return string representation of collection
   * @see #toString(org.sonar.wsclient.services.Violation)
   */
  public static String toString(Collection<Violation> violations) {
    StringBuilder sb = new StringBuilder().append('[');
    for (Violation violation : violations) {
      sb.append(toString(violation)).append(',');
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * TODO Godin: can we include this method into sonar-ws-client for debug purposes ?
   * 
   * @param violation violation to convert to string
   * @return string representation of violation
   * @see #toString(java.util.Collection)
   */
  public static String toString(Violation violation) {
    return new ToStringBuilder(violation)
        .append("message", violation.getMessage())
        .append("priority", violation.getSeverity())
        .append("line", violation.getLine())
        .toString();
  }

  /**
   * Hide utility-class constructor.
   */
  private ViolationUtils() {
  }
}
