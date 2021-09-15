/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarsource.sonarlint.core.client.api.common.QuickFix;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow;

public interface Trackable {

  @Nullable
  Long getMarkerId();

  void setMarkerId(@Nullable Long id);

  /**
   * The line index, starting with 1. Null means that
   * issue does not relate to a line (file issue for example).
   */
  @Nullable
  Integer getLine();

  @Nullable
  String getMessage();

  @Nullable
  Integer getTextRangeHash();

  @Nullable
  Integer getLineHash();

  String getRuleKey();

  String getRuleName();

  @Nullable
  Long getCreationDate();

  @Nullable
  String getServerIssueKey();

  boolean isResolved();

  // empty if none
  String getAssignee();

  /**
   * Can be overriden by server side issue in connected mode
   */
  String getSeverity();

  /**
   * Original severity reported by the analyzer
   */
  String getRawSeverity();

  /**
   * Can be overriden by server side issue in connected mode
   */
  String getType();

  /**
   * Original type reported by the analyzer
   */
  String getRawType();

  @Nullable
  TextRange getTextRange();

  List<Flow> getFlows();

  List<QuickFix> getQuickFix();

}
