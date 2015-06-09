/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.common.issues.ISonarIssueWithPath;

import java.util.Date;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
class RemoteSourceCode implements SourceCode {

  private final String key;
  private RemoteSonarIndex index;

  private String localContent;

  /**
   * Lazy initialization - see {@link #getDiff()}.
   */
  private SourceCodeDiff diff;

  /**
   * Lazy initialization - see {@link #getRemoteContentAsArray()}.
   */
  private String[] remoteContent;

  public RemoteSourceCode(String key) {
    this.key = key;
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode setLocalContent(final String content) {
    this.localContent = content;
    return this;
  }

  private String getLocalContent() {
    if (localContent == null) {
      return "";
    }
    return localContent;
  }

  private String[] getRemoteContentAsArray() {
    if (remoteContent == null) {
      remoteContent = getRemoteSonarIndex().getSonarClient().getRemoteCode(getKey());
    }
    return remoteContent;
  }

  public String getRemoteContent() {
    return StringUtils.join(getRemoteContentAsArray(), "\n");
  }

  private SourceCodeDiff getDiff() {
    if (diff == null) {
      diff = index.getDiffEngine().diff(SimpleSourceCodeDiffEngine.split(getLocalContent()), getRemoteContentAsArray());
    }
    return diff;
  }

  public List<ISonarIssue> getRemoteIssuesWithLineCorrection(IProgressMonitor monitor) {
    final List<ISonarIssueWithPath> issues = getRemoteSonarIndex().getSonarClient().getUnresolvedRemoteIssuesRecursively(getKey(), monitor);
    return IssuesUtils.convertLines(issues, getDiff());
  }

  public List<ISonarIssueWithPath> getRemoteIssuesRecursively(IProgressMonitor monitor) {
    return getRemoteSonarIndex().getSonarClient().getUnresolvedRemoteIssuesRecursively(getKey(), monitor);
  }

  /**
   * {@inheritDoc}
   */
  public int compareTo(final SourceCode resource) {
    return key.compareTo(resource.getKey());
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj instanceof RemoteSourceCode) && (key.equals(((RemoteSourceCode) obj).key));
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("key", key).toString();
  }

  protected RemoteSourceCode setRemoteSonarIndex(final RemoteSonarIndex index) {
    this.index = index;
    return this;
  }

  protected RemoteSonarIndex getRemoteSonarIndex() {
    return index;
  }

  public Date getAnalysisDate() {
    return index.getSonarClient().getLastAnalysisDate(key);
  }
}
