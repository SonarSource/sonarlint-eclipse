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
package org.sonar.ide.eclipse.core.internal.remote;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  /**
   * Lazy initialization - see {@link #getChildren()}.
   */
  private Set<SourceCode> children;

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
  public Set<SourceCode> getChildren() {
    if (children == null) {
      String[] childrenKeys = index.getSonarClient().getChildrenKeys(getKey());
      children = new HashSet<SourceCode>();
      for (String childKey : childrenKeys) {
        children.add(new RemoteSourceCode(childKey).setRemoteSonarIndex(index));
      }
    }
    return children;
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
    final List<ISonarIssue> issues = getRemoteSonarIndex().getSonarClient().getRemoteIssuesRecursively(getKey(), monitor);
    return IssuesUtils.convertLines(issues, getDiff());
  }

  public List<ISonarIssue> getRemoteIssuesRecursively(IProgressMonitor monitor) {
    return getRemoteSonarIndex().getSonarClient().getRemoteIssuesRecursively(getKey(), monitor);
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
