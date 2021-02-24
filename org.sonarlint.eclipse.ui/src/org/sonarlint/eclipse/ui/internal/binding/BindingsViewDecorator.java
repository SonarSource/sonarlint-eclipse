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
package org.sonarlint.eclipse.ui.internal.binding;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.RemoteSonarProject;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class BindingsViewDecorator extends LabelProvider implements ILightweightLabelDecorator {

  static final String ID = "org.sonarlint.eclipse.ui.navigatorDecorator";

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IConnectedEngineFacade) {
      IConnectedEngineFacade server = (IConnectedEngineFacade) element;
      addSuffix(decoration, server.getSonarLintStorageStateLabel());
    } else if (element instanceof RemoteSonarProject) {
      addSuffix(decoration, ((RemoteSonarProject) element).getProjectKey());
    } else if (element instanceof ISonarLintProject) {
      SonarLintProjectConfiguration projectConfig = SonarLintCorePlugin.loadConfig(((ISonarLintProject) element));
      projectConfig.getProjectBinding().ifPresent(b -> decoration.addSuffix(" /" + b.sqPathPrefix()));
    }
  }

  private static void addSuffix(IDecoration decoration, String suffix) {
    if (StringUtils.isNotBlank(suffix)) {
      decoration.addSuffix(" [" + suffix + "]");
    }
  }

}
