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
package org.sonarlint.eclipse.core.internal.tracking;

public abstract class MutableTrackableImpl implements MutableTrackable {

  private static final Long DEFAULT_CREATION_DATE = null;
  private static final String DEFAULT_SERVER_ISSUE_KEY = null;
  private static final boolean DEFAULT_RESOLVED = false;
  private static final String DEFAULT_ASSIGNEE = "";

  private Long creationDate = DEFAULT_CREATION_DATE;
  private String serverIssueKey = DEFAULT_SERVER_ISSUE_KEY;
  private boolean resolved = DEFAULT_RESOLVED;
  private String assignee = DEFAULT_ASSIGNEE;

  @Override
  public String getServerIssueKey() {
    return serverIssueKey;
  }

  @Override
  public Long getCreationDate() {
    return creationDate;
  }

  @Override
  public boolean isResolved() {
    return resolved;
  }

  @Override
  public String getAssignee() {
    return assignee;
  }

  @Override
  public void copyTrackedDetails(Trackable base) {
    creationDate = base.getCreationDate();
    serverIssueKey = base.getServerIssueKey();
    resolved = base.isResolved();
    assignee = base.getAssignee();
  }

  @Override
  public void resetTrackedDetails() {
    creationDate = DEFAULT_CREATION_DATE;
    serverIssueKey = DEFAULT_SERVER_ISSUE_KEY;
    resolved = DEFAULT_RESOLVED;
    assignee = DEFAULT_ASSIGNEE;
  }

  @Override
  public void setCreationDate(long currentTimeMillis) {
    creationDate = currentTimeMillis;
  }
}
