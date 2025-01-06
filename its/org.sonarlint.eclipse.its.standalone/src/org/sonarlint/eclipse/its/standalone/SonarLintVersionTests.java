/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.standalone;

import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.ReleaseNotesPreferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 *  These integration tests will later be moved to a specific bundle where only the information about "SonarLint
 *  installed / updated" (ergo Release Notes notification) and "New SonarLint version available" (ergo New Version
 *  notification) will be tested. This requires SonarLint starting up and nothing else.
 *
 *  This must be done on its own integration test runtime because we cannot restart Eclipse when running with Reddeer!
 */
public class SonarLintVersionTests extends AbstractSonarLintTest {
  @Test
  public void test_ReleaseNotes_preferences() {
    var releaseNotesPreferences = ReleaseNotesPreferences.open();

    await().untilAsserted(
      () -> assertThat(releaseNotesPreferences.getFlatTextContent())
        .contains("<h2>Older releases</h2>"));
  }
}
