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
package org.sonarlint.eclipse.core.internal.tracking.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.jdt.annotation.Nullable;

public class IssueMatcher {

  public static MatchingResults matchIssues(Collection<? extends MatchableIssue> leftIssues, Collection<? extends MatchableIssue> rightIssues) {
    var result = new MatchingResults(leftIssues, rightIssues);

    // 1. match issues with same rule, same line and same text range hash, but not necessarily with same message
    match(result, LineAndTextRangeHashKeyFactory.INSTANCE);

    // 2. match issues with same rule, same message and same text range hash
    match(result, TextRangeHashAndMessageKeyFactory.INSTANCE);

    // 3. match issues with same rule, same line and same message
    match(result, LineAndMessageKeyFactory.INSTANCE);

    // 4. match issues with same rule and same text range hash but different line and different message.
    match(result, TextRangeHashKeyFactory.INSTANCE);

    return result;
  }

  private static void match(MatchingResults results, SearchKeyFactory factory) {
    if (results.isComplete()) {
      return;
    }

    var rightSearch = new HashMap<SearchKey, List<MatchableIssue>>();
    for (var right : results.getUnmatchedRights()) {
      var searchKey = factory.apply(right);
      rightSearch.computeIfAbsent(searchKey, k -> new ArrayList<>()).add(right);
    }

    for (var left : results.getUnmatchedLefts()) {
      var leftKey = factory.apply(left);
      var rightCandidates = rightSearch.get(leftKey);
      if (rightCandidates != null && !rightCandidates.isEmpty()) {
        // TODO taking the first one. Could be improved if there are more than 2 issues on the same line.
        // Message could be checked to take the best one.
        var match = rightCandidates.iterator().next();
        results.match(left, match);
        rightSearch.get(leftKey).remove(match);
      }
    }
  }

  private interface SearchKey {
  }

  @FunctionalInterface
  private interface SearchKeyFactory extends Function<MatchableIssue, SearchKey> {
    @Override
    SearchKey apply(MatchableIssue trackable);
  }

  private static class LineAndTextRangeHashKey implements SearchKey {
    private final String ruleKey;
    @Nullable
    private final String textRangeHash;
    @Nullable
    private final Integer line;

    LineAndTextRangeHashKey(MatchableIssue matchable) {
      this.ruleKey = matchable.getRuleKey();
      this.line = matchable.getLine();
      this.textRangeHash = matchable.getTextRangeHash();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (this.getClass() != o.getClass()) {
        return false;
      }
      var that = (LineAndTextRangeHashKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && Objects.equals(textRangeHash, that.textRangeHash)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      var result = ruleKey.hashCode();
      result = 31 * result + (textRangeHash != null ? textRangeHash.hashCode() : 0);
      result = 31 * result + (line != null ? line.hashCode() : 0);
      return result;
    }
  }

  private enum LineAndTextRangeHashKeyFactory implements SearchKeyFactory {
    INSTANCE;

    @Override
    public SearchKey apply(MatchableIssue t) {
      return new LineAndTextRangeHashKey(t);
    }
  }

  private static class TextRangeHashAndMessageKey implements SearchKey {
    private final String ruleKey;
    @Nullable
    private final String message;
    @Nullable
    private final String textRangeHash;

    TextRangeHashAndMessageKey(MatchableIssue matchable) {
      this.ruleKey = matchable.getRuleKey();
      this.message = matchable.getMessage();
      this.textRangeHash = matchable.getTextRangeHash();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (this.getClass() != o.getClass()) {
        return false;
      }
      var that = (TextRangeHashAndMessageKey) o;
      // start with most discriminant field
      return Objects.equals(textRangeHash, that.textRangeHash)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      var result = ruleKey.hashCode();
      result = 31 * result + message.hashCode();
      result = 31 * result + (textRangeHash != null ? textRangeHash.hashCode() : 0);
      return result;
    }
  }

  private enum TextRangeHashAndMessageKeyFactory implements SearchKeyFactory {
    INSTANCE;

    @Override
    public SearchKey apply(MatchableIssue m) {
      return new TextRangeHashAndMessageKey(m);
    }
  }

  private static class LineAndMessageKey implements SearchKey {
    private final String ruleKey;
    @Nullable
    private final String message;
    @Nullable
    private final Integer line;

    LineAndMessageKey(MatchableIssue matchable) {
      this.ruleKey = matchable.getRuleKey();
      this.message = matchable.getMessage();
      this.line = matchable.getLine();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (this.getClass() != o.getClass()) {
        return false;
      }
      var that = (LineAndMessageKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      var result = ruleKey.hashCode();
      result = 31 * result + message.hashCode();
      result = 31 * result + (line != null ? line.hashCode() : 0);
      return result;
    }
  }

  private enum LineAndMessageKeyFactory implements SearchKeyFactory {
    INSTANCE;

    @Override
    public SearchKey apply(MatchableIssue m) {
      return new LineAndMessageKey(m);
    }
  }

  private static class TextRangeHashKey implements SearchKey {
    private final String ruleKey;
    @Nullable
    private final String textRangeHash;

    TextRangeHashKey(MatchableIssue matchable) {
      this.ruleKey = matchable.getRuleKey();
      this.textRangeHash = matchable.getTextRangeHash();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (this.getClass() != o.getClass()) {
        return false;
      }
      var that = (TextRangeHashKey) o;
      // start with most discriminant field
      return Objects.equals(textRangeHash, that.textRangeHash)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      var result = ruleKey.hashCode();
      result = 31 * result + (textRangeHash != null ? textRangeHash.hashCode() : 0);
      return result;
    }
  }

  private enum TextRangeHashKeyFactory implements SearchKeyFactory {
    INSTANCE;

    @Override
    public SearchKey apply(MatchableIssue m) {
      return new TextRangeHashKey(m);
    }
  }

}
