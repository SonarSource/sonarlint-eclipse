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
package org.sonarlint.eclipse.ui.internal.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.RemoteSonarProject;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;

public class BindingsViewDecorator extends LabelProvider implements ILightweightLabelDecorator {

  static final String ID = "org.sonarlint.eclipse.ui.navigatorDecorator";

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof RemoteSonarProject) {
      addSuffix(decoration, ((RemoteSonarProject) element).getProjectKey());
    } else if (element instanceof ISonarLintProject) {
      var project = (ISonarLintProject) element;
      var projectConfig = SonarLintCorePlugin.loadConfig(project);
      projectConfig.getProjectBinding().ifPresent(eclipseProjectBinding -> {
        List<String> infos = new ArrayList<>();
        VcsService.getServerBranch(project).ifPresent(b -> infos.add("Branch: '" + b + "'"));

        appendNewCodePeriod(project, infos);

        if (StringUtils.isNotBlank(eclipseProjectBinding.idePathPrefix())) {
          infos.add("IDE directory: '/" + eclipseProjectBinding.idePathPrefix() + "'");
        }
        if (StringUtils.isNotBlank(eclipseProjectBinding.serverPathPrefix())) {
          infos.add("Sonar directory: '/" + eclipseProjectBinding.serverPathPrefix() + "'");
        }
        addSuffix(decoration, infos.stream().collect(Collectors.joining(", ")));
      });
    }
  }

  private static void appendNewCodePeriod(ISonarLintProject project, List<String> infos) {
    // Should probably be cached once moving to RPC
    try {
      var response = SonarLintBackendService.get().getNewCodeDefinition(project).get();
      if (response.isSupported()) {
        infos.add("New Code Period: '" + response.getDescription() + "'");
      }
    } catch (ExecutionException e) {
      SonarLintLogger.get().debug("Unable to get new code definition", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void addSuffix(IDecoration decoration, String suffix) {
    if (StringUtils.isNotBlank(suffix)) {
      decoration.addSuffix(" [" + suffix + "]");
    }
  }

}
