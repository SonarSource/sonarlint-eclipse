/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
import java.util.Optional;
import org.eclipse.jgit.annotations.Nullable;
import org.sonarsource.sonarlint.core.analysis.api.Flow;
import org.sonarsource.sonarlint.core.analysis.api.QuickFix;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;
import org.sonarsource.sonarlint.core.commons.TextRange;

public class WrappedTrackable implements Trackable {

  private final Trackable trackable;

  public WrappedTrackable(Trackable trackable) {
    this.trackable = trackable;
  }

  @Nullable
  @Override
  public Long getMarkerId() {
    return trackable.getMarkerId();
  }

  @Override
  public void setMarkerId(Long id) {
    trackable.setMarkerId(id);
  }

  @Nullable
  @Override
  public Integer getLine() {
    return trackable.getLine();
  }

  @Nullable
  @Override
  public String getMessage() {
    return trackable.getMessage();
  }

  @Nullable
  @Override
  public Integer getTextRangeHash() {
    return trackable.getTextRangeHash();
  }

  @Nullable
  @Override
  public Integer getLineHash() {
    return trackable.getLineHash();
  }

  @Override
  public String getRuleKey() {
    return trackable.getRuleKey();
  }

  @Nullable
  @Override
  public IssueSeverity getSeverity() {
    return trackable.getSeverity();
  }

  @Override
  public IssueSeverity getRawSeverity() {
    return trackable.getRawSeverity();
  }

  @Override
  public RuleType getType() {
    return trackable.getType();
  }

  @Override
  public RuleType getRawType() {
    return trackable.getRawType();
  }

  @Nullable
  @Override
  public TextRange getTextRange() {
    return trackable.getTextRange();
  }

  @Nullable
  @Override
  public String getServerIssueKey() {
    return trackable.getServerIssueKey();
  }

  @Nullable
  @Override
  public Long getCreationDate() {
    return trackable.getCreationDate();
  }

  @Override
  public boolean isResolved() {
    return trackable.isResolved();
  }

  @Override
  public List<Flow> getFlows() {
    return trackable.getFlows();
  }

  @Override
  public List<QuickFix> getQuickFix() {
    return trackable.getQuickFix();
  }

  @Override
  public Optional<String> getRuleDescriptionContextKey() {
    return trackable.getRuleDescriptionContextKey();
  }
}
