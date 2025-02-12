/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarLintProjectConfigurationManagerTest extends SonarTestCase {
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
      public void error(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
        var stack = new StringWriter();
        t.printStackTrace(new PrintWriter(stack));
        error(msg, fromAnalyzer);
        error(stack.toString(), fromAnalyzer);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
        // We ignore debug messages in UTs
      }

      @Override
      public void debug(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
        // We ignore debug messages in UTs
      }

      @Override
      public void traceIdeMessage(@Nullable String msg) {
        // INFO: We ignore Eclipse-specific tracing in UTs
      }

      @Override
      public void traceIdeMessage(@Nullable String msg, Throwable t) {
        // INFO: We ignore Eclipse-specific tracing in UTs
      }
    });
  }

  @Test
  public void settings_are_written_to_disk() throws IOException, CoreException, InterruptedException {
    var project = importEclipseProject("SimpleProject");
    var projectScope = new ProjectScope(project);
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).doesNotExist();
    var configuration = SonarLintCorePlugin.getInstance().getProjectConfigManager().load(projectScope);
    configuration.setAutoEnabled(false);
    configuration.setBindingSuggestionsDisabled(true);
    configuration.setProjectBinding(new EclipseProjectBinding("myServer", "myProjectKey"));
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).doesNotExist();
    SonarLintCorePlugin.getInstance().getProjectConfigManager().save(projectScope, configuration);
    assertThat(projectScope.getLocation().append("org.sonarlint.eclipse.core.prefs").toFile()).exists();
  }
}
