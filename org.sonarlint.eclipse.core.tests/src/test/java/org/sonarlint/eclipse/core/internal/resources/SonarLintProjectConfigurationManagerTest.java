/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarLintProjectConfigurationManagerTest extends SonarTestCase {

  private static final List<String> infos = new ArrayList<>();
  private static final List<String> errors = new ArrayList<>();

  @BeforeClass
  public static void prepare() throws Exception {
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
  public void load_deprecated_project_config() throws IOException, CoreException {
    IProject project = importEclipseProject("DeprecatedModuleBinding");
    // Configure the project
    SonarLintProjectConfiguration configuration = SonarLintCorePlugin.getInstance().getProjectConfigManager().load(new ProjectScope(project), "Deprecated Binding Project");
    assertThat(configuration.getProjectBinding()).isEmpty();
    assertThat(errors).isEmpty();
    assertThat(infos).containsExactly("Binding configuration of project 'Deprecated Binding Project' is outdated. Please rebind this project.");
  }
}
