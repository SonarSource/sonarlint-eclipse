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

import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;

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
  private static final String GRADLE_MAVEN_MIXED_PROJECT = "MixedProjectMavenSide";

  private static final String SHELL_NAME_SONARQUBE = "Connection Suggestion to SonarQube Server";
  private static final String SHELL_NAME_SONARCLOUD = "Connection Suggestion to SonarQube Cloud";
  private static final String SHELL_NAME_MULTIPLE = "SonarQube - Multiple Connection Suggestions found";

  // When one test fails it shouldn't let following tests fail due to the pop-ups staying.
  @After
  public void closeLeftoverShells() {
    shellByName(SHELL_NAME_SONARQUBE).ifPresent(DefaultShell::close);
    shellByName(SHELL_NAME_SONARCLOUD).ifPresent(DefaultShell::close);
    shellByName(SHELL_NAME_MULTIPLE).ifPresent(DefaultShell::close);
  }

  @Test
  public void single_project_Gradle() {
    new JavaPerspective().open();

    importExistingGradleProjectIntoWorkspace("java/gradle-project", GRADLE_PROJECT);

    var shellOpt = shellByName(SHELL_NAME_SONARQUBE);
    try {
      assertThat(shellOpt).isNotEmpty();
      var shell = shellOpt.get();

      assertThat(getNotificationText(shell))
        .contains(LOCAL_SONARQUBE);

      // Sadly the Gradle integration is very slow and error-prone here. Not every time it sees the projects being
      // connected and therefore loads them sometimes correctly as one project, sometimes as two projects with a job
      // joining them together afterwards. The second case is too slow for the SonarLint backend and therefore flaky on
      // the CI (but can be witnessed locally as well sometimes), that's why we catch it here just in case: The
      // SonarLint integration is working correctly as expected in both situations, the Gradle integration just isn't.
      assertThat(getNotificationText(shell)).satisfiesAnyOf(
        list -> assertThat(list).contains("local project '" + GRADLE_ROOT_PROJECT),
        list -> assertThat(list).contains("local project '" + GRADLE_SUB_PROJECT),
        list -> assertThat(list).contains("local project '" + GRADLE_PROJECT));
    } finally {
      shellOpt.ifPresent(shell -> {
        if (!shell.getControl().isDisposed()) {
          shell.close();
        }
      });
    }
    shellByName(SHELL_NAME_SONARQUBE).ifPresent(DefaultShell::close);
  }

  @Test
  public void multi_project_Gradle() {
    new JavaPerspective().open();

    importExistingGradleProjectIntoWorkspace("java/gradle-root-project", GRADLE_ROOT_PROJECT);

    var firstShell = shellByName(SHELL_NAME_SONARQUBE);
    try {
      assertThat(firstShell).isNotEmpty();
      assertThat(getNotificationText(firstShell.get()))
        .contains(LOCAL_SONARQUBE)
        .contains(GRADLE_GROUP + ":" + GRADLE_ROOT_PROJECT);

      // Sadly the Gradle integration is very slow and error-prone here. Not every time it sees the projects being
      // connected and therefore loads them sometimes correctly as one project, sometimes as two projects with a job
      // joining them together afterwards. The second case is too slow for the SonarLint backend and therefore flaky on
      // the CI (but can be witnessed locally as well sometimes), that's why we catch it here just in case: The
      // SonarLint integration is working correctly as expected in both situations, the Gradle integration just isn't.
      if (!getNotificationText(firstShell.get()).contains("multiple local projects")) {
        assertThat(getNotificationText(firstShell.get())).satisfiesAnyOf(
          list -> assertThat(list).contains("local project '" + GRADLE_ROOT_PROJECT),
          list -> assertThat(list).contains("local project '" + GRADLE_SUB_PROJECT));
      }
    } finally {
      firstShell.ifPresent(shell -> {
        if (!shell.getControl().isDisposed()) {
          shell.close();
        }
      });
    }
    shellByName(SHELL_NAME_SONARQUBE).ifPresent(DefaultShell::close);
  }

  // Mixed Gradle/Maven project containing two different shared Connected Mode configurations
  @Test
  public void mixed_Gradle_Maven_project() {
    new JavaPerspective().open();

    importExistingProjectIntoWorkspace("java/gradle-maven-mixed", GRADLE_MAVEN_MIXED_PROJECT);

    var shellOpt = shellByName(SHELL_NAME_MULTIPLE);
    try {
      assertThat(shellOpt).isNotEmpty();

      assertThat(getNotificationText(shellOpt.get()))
        .contains("different suggestions")
        .contains("local project '" + GRADLE_MAVEN_MIXED_PROJECT);

      // Informational dialog for users
      new DefaultLink(shellOpt.get(), "More information").click();
      shellByName("Connection suggestions for Eclipse project '" + GRADLE_MAVEN_MIXED_PROJECT + "'")
        .ifPresent(DefaultShell::close);

      // Actual choosing dialog for users
      new DefaultLink(shellOpt.get(), "Choose suggestion").click();
      shellByName("Choose suggestion for Eclipse project '" + GRADLE_MAVEN_MIXED_PROJECT + "'")
        .ifPresent(DefaultShell::close);
    } finally {
      shellOpt.ifPresent(shell -> {
        if (!shell.getControl().isDisposed()) {
          shell.close();
        }
      });
    }
    shellByName(SHELL_NAME_MULTIPLE).ifPresent(DefaultShell::close);
  }
}
