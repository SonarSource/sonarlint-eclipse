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
package org.sonarlint.eclipse.core.internal.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarLintProjectConfigurationManagerTest extends SonarTestCase {

  private static final String PROJECT_WITH_DEPRECATED_SETTINGS = "DeprecatedModuleBinding";
  private final List<String> infos = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  @Before
  public void prepare() throws Exception {
    SonarLintLogger.get().addLogListener(new LogListener() {
      @Override
      public void info(String msg, boolean fromAnalyzer) {
        infos.add(msg);
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
        errors.add(msg);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
      }

    });

  }

  @Test
  public void load_deprecated_project_config() throws IOException, CoreException, InterruptedException {
    IProject project = importEclipseProject(PROJECT_WITH_DEPRECATED_SETTINGS);
    // Configure the project
    SonarLintProjectConfiguration configuration = SonarLintCorePlugin.getInstance().getProjectConfigManager().load(new ProjectScope(project), PROJECT_WITH_DEPRECATED_SETTINGS);
    assertThat(configuration.getProjectBinding()).isEmpty();
    assertThat(errors).isEmpty();
    assertThat(infos).contains("Binding configuration of project '" + PROJECT_WITH_DEPRECATED_SETTINGS + "' is outdated. Please rebind this project.");
  }

  @Test
  public void settings_are_written_to_disk() throws IOException, CoreException, InterruptedException {
    IProject project = importEclipseProject("SimpleProject");
    ProjectScope projectScope = new ProjectScope(project);
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).doesNotExist();
    SonarLintProjectConfiguration configuration = SonarLintCorePlugin.getInstance().getProjectConfigManager().load(projectScope, "SimpleProject");
    configuration.setAutoEnabled(false);
    configuration.setProjectBinding(new EclipseProjectBinding("myServer", "myProjectKey", "aPrefix", "aSuffix"));
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).doesNotExist();
    SonarLintCorePlugin.getInstance().getProjectConfigManager().save(projectScope, configuration);
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).exists();
    assertThat(errors).isEmpty();

  }
}
