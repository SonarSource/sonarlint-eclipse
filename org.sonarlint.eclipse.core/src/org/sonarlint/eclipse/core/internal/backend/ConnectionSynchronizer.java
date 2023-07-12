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
package org.sonarlint.eclipse.core.internal.backend;

import java.util.List;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacadeLifecycleListener;
import org.sonarsource.sonarlint.core.clientapi.SonarLintBackend;
import org.sonarsource.sonarlint.core.clientapi.backend.connection.config.DidUpdateConnectionsParams;
import org.sonarsource.sonarlint.core.clientapi.backend.connection.config.SonarCloudConnectionConfigurationDto;
import org.sonarsource.sonarlint.core.clientapi.backend.connection.config.SonarQubeConnectionConfigurationDto;

import static java.util.stream.Collectors.toList;

class ConnectionSynchronizer implements IConnectedEngineFacadeLifecycleListener {

  private final SonarLintBackend backend;

  public ConnectionSynchronizer(SonarLintBackend backend) {
    this.backend = backend;
  }

  @Override
  public void connectionRemoved(IConnectedEngineFacade facade) {
    didUpdateConnections();
  }

  @Override
  public void connectionChanged(IConnectedEngineFacade facade) {
    didUpdateConnections();
  }

  @Override
  public void connectionAdded(IConnectedEngineFacade facade) {
    didUpdateConnections();
  }

  private void didUpdateConnections() {
    var sqConnections = buildSqConnectionDtos();
    var scConnections = buildScConnectionDtos();
    backend.getConnectionService().didUpdateConnections(new DidUpdateConnectionsParams(sqConnections, scConnections));
  }

  static List<SonarQubeConnectionConfigurationDto> buildSqConnectionDtos() {
    return SonarLintCorePlugin.getServersManager().getServers().stream()
      .filter(c -> !c.isSonarCloud())
      .map(c -> new SonarQubeConnectionConfigurationDto(c.getId(), c.getHost(), c.areNotificationsDisabled()))
      .collect(toList());
  }

  static List<SonarCloudConnectionConfigurationDto> buildScConnectionDtos() {
    return SonarLintCorePlugin.getServersManager().getServers().stream()
      .filter(IConnectedEngineFacade::isSonarCloud)
      .map(c -> new SonarCloudConnectionConfigurationDto(c.getId(), c.getOrganization(), c.areNotificationsDisabled()))
      .collect(toList());
  }
}
