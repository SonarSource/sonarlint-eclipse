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

import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.junit.Test;

import static org.assertj.core.api.Assertions.tuple;

public class SecretsTest extends AbstractSonarLintTest {
  @Test
  public void shouldFindSecretsInTextFiles() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-in-text-file", "secret-in-text-file");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("secret"));
    waitForMarkers(new DefaultEditor(),
      tuple("Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.", 3));

    new DefaultLink(shellByName("SonarLint - Secret(s) detected").get(), "Dismiss").click();
  }

  @Test
  public void shouldFindSecretsInSourceFiles() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-java", "secret-java");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "sec", "Secret.java"));
    waitForMarkers(new DefaultEditor(),
      tuple("Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.", 4));

    new DefaultLink(shellByName("SonarLint - Secret(s) detected").get(), "Dismiss").click();
  }

  @Test
  public void shouldNotTriggerAnalysisForGitIgnoredFiles() throws CoreException {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-gitignored", "secret-gitignored");

    var workspace = ResourcesPlugin.getWorkspace();
    final var iProject = workspace.getRoot().getProject("secret-gitignored");

    var file = iProject.getFile(new Path("secret.txt"));
    file.create(new ByteArrayInputStream("AWS_SECRET_KEY: h1ByXvzhN6O8/UQACtwMuSkjE5/oHmWG1MJziTDw".getBytes()), true, null);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("secret.txt"));
    waitForNoMarkers(new DefaultEditor());
  }
}
