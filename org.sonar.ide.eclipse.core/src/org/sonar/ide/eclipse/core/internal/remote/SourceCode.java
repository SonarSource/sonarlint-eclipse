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
package org.sonar.ide.eclipse.core.internal.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.common.issues.ISonarIssueWithPath;

import java.util.Date;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public interface SourceCode extends Comparable<SourceCode> {

  String getKey();

  /**
   * @return content, which was analyzed by Sonar
   */
  String getRemoteContent();

  /**
   * HIGHLY EXPERIMENTAL!!!
   *
   * @param content content of this resource
   * @return this (for method chaining)
   */
  SourceCode setLocalContent(String content);

  Date getAnalysisDate();

  List<ISonarIssue> getRemoteIssuesWithLineCorrection(IProgressMonitor monitor);

  List<ISonarIssueWithPath> getRemoteIssuesRecursively(IProgressMonitor monitor);

}
