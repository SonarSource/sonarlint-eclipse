/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.tracking.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MatchingResults {

  /**
   * Matched issues -> a left issue is associated to a right issue
   */
  private final IdentityHashMap<MatchableIssue, MatchableIssue> leftToRight = new IdentityHashMap<>();
  private final IdentityHashMap<MatchableIssue, MatchableIssue> rightToLeft = new IdentityHashMap<>();

  private final Collection<MatchableIssue> lefts;
  private final Collection<MatchableIssue> rights;

  public MatchingResults(Collection<? extends MatchableIssue> lefts, Collection<? extends MatchableIssue> rights) {
    this.lefts = List.copyOf(lefts);
    this.rights = List.copyOf(rights);
  }

  /**
   * Returns an Iterable to be traversed when matching issues. That means
   * that the traversal does not fail if method {@link #match(MatchableIssue, MatchableIssue)}
   * is called.
   */
  public Iterable<MatchableIssue> getUnmatchedLefts() {
    var result = new ArrayList<MatchableIssue>();
    for (var r : lefts) {
      if (!leftToRight.containsKey(r)) {
        result.add(r);
      }
    }
    return result;
  }

  public Map<MatchableIssue, MatchableIssue> getMatchedRaws() {
    return leftToRight;
  }

  public MatchableIssue baseFor(MatchableIssue left) {
    return leftToRight.get(left);
  }

  /**
   * The base issues that are not matched by a raw issue and that need to be closed.
   */
  public Iterable<MatchableIssue> getUnmatchedRights() {
    var result = new ArrayList<MatchableIssue>();
    for (var b : rights) {
      if (!rightToLeft.containsKey(b)) {
        result.add(b);
      }
    }
    return result;
  }

  boolean containsUnmatchedBase(MatchableIssue base) {
    return !rightToLeft.containsKey(base);
  }

  void match(MatchableIssue left, MatchableIssue right) {
    leftToRight.put(left, right);
    rightToLeft.put(right, left);
  }

  boolean isComplete() {
    return leftToRight.size() == lefts.size() || rightToLeft.size() == rights.size();
  }

}
