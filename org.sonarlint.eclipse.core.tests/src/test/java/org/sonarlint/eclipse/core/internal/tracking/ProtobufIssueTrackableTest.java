/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import org.sonarlint.eclipse.core.internal.proto.Sonarlint.Issues.Issue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtobufIssueTrackableTest {

  private final Trackable empty = new ProtobufIssueTrackable(Issue.newBuilder().build());

  @Test
  public void should_return_null_serverIssueKey_when_unset() {
    assertThat(empty.getServerIssueKey()).isNull();
  }

  @Test
  public void should_return_null_line_when_unset() {
    assertThat(empty.getLine()).isNull();
  }

  @Test
  public void should_return_null_creationDate_when_unset() {
    assertThat(empty.getCreationDate()).isNull();
  }
}
