/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.SonarLintBackendImpl;
import org.sonarsource.sonarlint.core.clientapi.SonarLintBackend;
import org.sonarsource.sonarlint.core.clientapi.SonarLintClient;
import org.sonarsource.sonarlint.core.clientapi.backend.InitializeParams;
import org.sonarsource.sonarlint.core.clientapi.backend.connection.config.SonarCloudConnectionConfigurationDto;
import org.sonarsource.sonarlint.core.clientapi.backend.connection.config.SonarQubeConnectionConfigurationDto;
import org.sonarsource.sonarlint.core.commons.Language;

import static java.util.Objects.requireNonNull;

public class SonarLintBackendService {

  private static final SonarLintBackendService INSTANCE = new SonarLintBackendService();

  @Nullable
  private SonarLintBackend backend;

  public static SonarLintBackendService get() {
    return INSTANCE;
  }

  public void init(SonarLintClient client) {
    SonarLintLogger.get().debug("Initializing SonarLint backend...");

    this.backend = new SonarLintBackendImpl(client);
    var nodeJsManager = SonarLintCorePlugin.getNodeJsManager();

    List<Path> embeddedPluginPaths = PluginPathHelper.getEmbeddedPluginPaths();
    embeddedPluginPaths.stream().forEach(p -> SonarLintLogger.get().debug("  - " + p));

    Map<String, Path> extraPlugins = new HashMap<>();
    var secretsPluginUrl = PluginPathHelper.findEmbeddedSecretsPlugin();
    if (secretsPluginUrl != null) {
      extraPlugins.put(Language.SECRETS.getPluginKey(), secretsPluginUrl);
    }

    Map<String, Path> embeddedPlugins = new HashMap<>();
    embeddedPlugins.put(Language.JS.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedJsPlugin(), "JS/TS plugin not found"));
    embeddedPlugins.put(Language.HTML.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedHtmlPlugin(), "HTML plugin not found"));
    embeddedPlugins.put(Language.XML.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedXmlPlugin(), "XML plugin not found"));

    var connections = SonarLintCorePlugin.getServersManager().getServers();
    List<SonarQubeConnectionConfigurationDto> sqConnections = new ArrayList<>();
    List<SonarCloudConnectionConfigurationDto> scConnections = new ArrayList<>();
    connections.forEach(c -> {
      if (c.isSonarCloud()) {
        scConnections.add(new SonarCloudConnectionConfigurationDto(c.getId(), c.getOrganization()));
      } else {
        sqConnections.add(new SonarQubeConnectionConfigurationDto(c.getId(), c.getHost()));
      }
    });

    backend.initialize(new InitializeParams("eclipse", StoragePathManager.getServerStorageRoot(),
      Set.copyOf(embeddedPluginPaths),
      extraPlugins,
      embeddedPlugins,
      SonarLintUtils.getEnabledLanguages(),
      SonarLintUtils.getEnabledLanguages(),
      nodeJsManager.getNodeJsVersion(),
      false,
      sqConnections,
      scConnections,
      null));
  }

  public SonarLintBackend getBackend() {
    return requireNonNull(backend, "SonarLintBackendService has not been initialized");
  }

  public void stop() {
    if (backend != null) {
      try {
        backend.shutdown().get(10, TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException e) {
        Platform.getLog(SonarLintBackendService.class).error("Unable to stop the SonartLint backend", e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    backend = null;
  }

}
