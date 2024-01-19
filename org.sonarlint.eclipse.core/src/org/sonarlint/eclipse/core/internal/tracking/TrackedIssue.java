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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.UUID;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.sonarlint.eclipse.core.internal.tracking.matching.MatchableIssue;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.LocalOnlyIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.ServerMatchedIssueDto;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;

public class TrackedIssue implements MatchableIssue {

  private RawIssueTrackable raw;
  private boolean resolved = false;
  @Nullable
  private UUID id;
  @Nullable
  private Long markerId;
  @Nullable
  private RuleType overridenIssueType;
  @Nullable
  private IssueSeverity overridenIssueSeverity;
  @Nullable
  private Long creationDate;
  @Nullable
  private String serverIssueKey;
  private boolean isNewCode = true;

  /**
   * Fresh new issue from analysis
   */
  private TrackedIssue(RawIssueTrackable raw, @Nullable Long creationDate) {
    this.creationDate = creationDate;
    this.raw = raw;
  }

  public static TrackedIssue asNew(RawIssueTrackable raw) {
    return new TrackedIssue(raw, System.currentTimeMillis());
  }

  public static TrackedIssue asUnknownCreationDate(RawIssueTrackable raw) {
    return new TrackedIssue(raw, null);
  }

  public static TrackedIssue fromPersistentCache(ProtobufMatchableIssueAdapter persistedIssue, RawIssueTrackable raw) {
    var pbIssue = persistedIssue.getProtobufIssue();
    var creationDate = pbIssue.getCreationDate();
    var issue = new TrackedIssue(raw, creationDate == 0 ? null : creationDate);
    issue.resolved = pbIssue.getResolved();
    var persistedSeverity = pbIssue.getSeverity();
    if (StringUtils.isNotBlank(persistedSeverity)) {
      issue.overridenIssueSeverity = IssueSeverity.valueOf(persistedSeverity);
    }
    var persistedType = pbIssue.getType();
    if (StringUtils.isNotBlank(persistedType)) {
      issue.overridenIssueType = RuleType.valueOf(persistedType);
    }
    var persistedServerIssueKey = pbIssue.getServerIssueKey();
    if (StringUtils.isNotBlank(persistedServerIssueKey)) {
      issue.serverIssueKey = persistedServerIssueKey;
    }
    issue.isNewCode = pbIssue.getIsOnNewCode();
    return issue;
  }

  public Issue getIssueFromAnalysis() {
    return raw.getIssueFromAnalysis();
  }

  @Override
  public @Nullable Integer getLine() {
    return raw.getLine();
  }

  @Override
  public @Nullable String getMessage() {
    return raw.getMessage();
  }

  @Override
  @Nullable
  public String getTextRangeHash() {
    return raw.getTextRangeHash();
  }

  @Nullable
  public String getLineHash() {
    return raw.getLineHash();
  }

  @Override
  public String getRuleKey() {
    return raw.getRuleKey();
  }

  public boolean isResolved() {
    return resolved;
  }
  
  public boolean isNewCode() {
    return isNewCode;
  }
  
  @Nullable
  public UUID getId() {
    return id;
  }
  
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  @Nullable
  public Long getMarkerId() {
    return markerId;
  }

  public void setMarkerId(@Nullable Long markerId) {
    this.markerId = markerId;
  }

  public RuleType getType() {
    return overridenIssueType != null ? overridenIssueType : raw.getIssueFromAnalysis().getType();
  }

  @Nullable
  public RuleType getOverridenIssueType() {
    return overridenIssueType;
  }

  public IssueSeverity getSeverity() {
    return overridenIssueSeverity != null ? overridenIssueSeverity : raw.getIssueFromAnalysis().getSeverity();
  }

  @Nullable
  public IssueSeverity getOverridenIssueSeverity() {
    return overridenIssueSeverity;
  }

  @Nullable
  public Long getCreationDate() {
    return creationDate;
  }

  @Nullable
  public String getServerIssueKey() {
    return serverIssueKey;
  }

  public void updateFromFreshAnalysis(RawIssueTrackable raw) {
    this.raw = raw;
  }

  public void updateFromSlCoreMatching(Either<ServerMatchedIssueDto, LocalOnlyIssueDto> resultIssue) {
    resultIssue.map(serverMatched -> {
      this.id = serverMatched.getId();
      this.creationDate = serverMatched.getIntroductionDate();
      this.overridenIssueSeverity = serverMatched.getOverriddenSeverity();
      this.overridenIssueType = serverMatched.getType();
      this.serverIssueKey = serverMatched.getServerKey();
      this.resolved = serverMatched.isResolved();
      this.isNewCode = serverMatched.isOnNewCode();
      return null;
    }, localMatched -> {
      this.id = localMatched.getId();
      this.overridenIssueSeverity = null;
      this.overridenIssueType = null;
      this.serverIssueKey = null;
      this.resolved = localMatched.getResolutionStatus() != null;
      this.isNewCode = true;
      return null;
    });
  }

}
