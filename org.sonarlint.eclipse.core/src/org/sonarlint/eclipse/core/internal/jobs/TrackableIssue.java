/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.jobs;

import org.sonarlint.eclipse.core.internal.markers.SonarMarker;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

class TrackableIssue implements Trackable {

  private final Issue issue;
  private final Integer lineHash;

  public TrackableIssue(Issue issue, Integer lineHash) {
    this.issue = issue;
    this.lineHash = lineHash;
  }

  public Issue getWrapped() {
    return issue;
  }

  @Override
  public Integer getLine() {
    return issue.getStartLine();
  }

  @Override
  public String getMessage() {
    return SonarMarker.getMessage(issue);
  }

  @Override
  public Integer getLineHash() {
    return lineHash;
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

}
