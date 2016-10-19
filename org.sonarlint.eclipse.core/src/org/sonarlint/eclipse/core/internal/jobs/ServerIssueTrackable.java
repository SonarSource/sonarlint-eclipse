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

import java.time.Instant;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

public class ServerIssueTrackable implements Trackable {

  private final ServerIssue serverIssue;

  public ServerIssueTrackable(ServerIssue serverIssue) {
    this.serverIssue = serverIssue;
  }

  @Override
  public Integer getLine() {
    return serverIssue.line();
  }

  @Override
  public String getMessage() {
    return serverIssue.message();
  }

  @Override
  public Integer getLineHash() {
    return serverIssue.checksum().hashCode();
  }

  @Override
  public String getRuleKey() {
    return serverIssue.ruleKey();
  }

  public Long getCreationDate() {
    return serverIssue.creationDate().toEpochMilli();
  }

  public String getServerIssueKey() {
    return serverIssue.key();
  }

  public boolean isResolved() {
    return !serverIssue.resolution().isEmpty();
  }

  public String getAssignee() {
    return serverIssue.assigneeLogin();
  }

  // TODO remove all this crap before PR

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String ruleKey;
    private String resolution;
    private String message;
    private int line;
    private String key;
    private Instant date;
    private String checksum;
    private String assignee;

    public ServerIssueTrackable build() {
      ServerIssue serverIssue = new ServerIssue() {
        @Override
        public String severity() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String ruleKey() {
          return ruleKey;
        }

        @Override
        public String resolution() {
          return resolution;
        }

        @Override
        public String moduleKey() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String message() {
          return message;
        }

        @Override
        public boolean manualSeverity() {
          throw new UnsupportedOperationException();
        }

        @Override
        public int line() {
          return line;
        }

        @Override
        public String key() {
          return key;
        }

        @Override
        public String filePath() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Instant creationDate() {
          return date;
        }

        @Override
        public String checksum() {
          return checksum;
        }

        @Override
        public String assigneeLogin() {
          return assignee;
        }
      };
      return new ServerIssueTrackable(serverIssue);
    }

    public Builder ruleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public Builder resolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder line(int line) {
      this.line = line;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder date(Instant date) {
      this.date = date;
      return this;
    }

    public Builder checksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder assignee(String assignee) {
      this.assignee = assignee;
      return this;
    }
  }
}
