/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarlint.eclipse.its;

import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  This tests the Shared Connected Mode configuration without any running orchestrator. This should be used to test
 *  everything before the actual connection (e.g. different Hierarchy Providers) without the need of starting up an
 *  orchestrator instance.
 *
 *  INFO: We can test that the notification is available, we sadly cannot test the content displayed in the notification!
 */
public class StandaloneSharedConnectedModeTest extends AbstractSonarLintTest {
  private static final String LOCAL_SONARQUBE = "http://localhost:9000";
  private static final String GRADLE_GROUP = "org.sonarlint.its.gradle";
  private static final String GRADLE_PROJECT = "gradle-project";
  private static final String GRADLE_ROOT_PROJECT = "gradle-root-project";
  private static final String GRADLE_SUB_PROJECT = "gradle-sub-project";

  @Test
  public void single_project_Gradle() {
    new JavaPerspective().open();

    importExistingGradleProjectIntoWorkspace("java/gradle-project", GRADLE_PROJECT);

    var shellOpt = shellByName("SonarLint Connection Suggestion to SonarQube");
    try {
      assertThat(shellOpt).isNotEmpty();

      assertThat(getSuggestionNotificationText(shellOpt.get()))
        .contains(LOCAL_SONARQUBE)
        .contains(GRADLE_GROUP + ":" + GRADLE_PROJECT)
        .contains("local project '" + GRADLE_PROJECT);
    } finally {
      shellOpt.ifPresent(DefaultShell::close);
    }
  }

  @Test
  public void multi_project_Gradle() {
    new JavaPerspective().open();

    importExistingGradleProjectIntoWorkspace("java/gradle-root-project", GRADLE_ROOT_PROJECT);

    var firstShell = shellByName("SonarLint Connection Suggestion to SonarQube");
    try {
      assertThat(firstShell).isNotEmpty();
      assertThat(getSuggestionNotificationText(firstShell.get()))
        .contains(LOCAL_SONARQUBE)
        .contains(GRADLE_GROUP + ":" + GRADLE_ROOT_PROJECT);

      // Sadly the Gradle integration is very slow and error-prone here. Not every time it sees the projects being
      // connected and therefore loads them sometimes correctly as one project, sometimes as two projects with a job
      // joining them together afterwards. The second case is too slow for the SonarLint backend and therefore flaky on
      // the CI (but can be witnessed locally as well sometimes), that's why we catch it here just in case: The
      // SonarLint integration is working correctly as expected in both situations, the Gradle integration just isn't.
      if (!getSuggestionNotificationText(firstShell.get()).contains("multiple local projects")) {
        assertThat(getSuggestionNotificationText(firstShell.get())).satisfiesAnyOf(
          list -> assertThat(list).contains("local project '" + GRADLE_ROOT_PROJECT),
          list -> assertThat(list).contains("local project '" + GRADLE_SUB_PROJECT));
      }
    } finally {
      firstShell.ifPresent(DefaultShell::close);
      shellByName("SonarLint Connection Suggestion to SonarQube").ifPresent(DefaultShell::close);
    }
  }

  /** On the these notifications the "content" is always the fourth label (index 3), don't ask me why! */
  private static String getSuggestionNotificationText(DefaultShell shell) {
    return new DefaultLabel(shell, 3).getText();
  }
}
