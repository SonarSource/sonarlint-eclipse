/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.telemetry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.common.TelemetryClientConfig;
import org.sonarsource.sonarlint.core.telemetry.Telemetry;
import org.sonarsource.sonarlint.core.telemetry.TelemetryPathManager;

public class SonarLintTelemetry implements AnalysisListener {
  private static final String TELEMETRY_PRODUCT_KEY = "eclipse";
  private static final String PRODUCT = "SonarLint Eclipse";
  private static final String OLD_STORAGE_FILENAME = "sonarlint_usage";
  public static final String DISABLE_PROPERTY_KEY = "sonarlint.telemetry.disabled";
  private boolean enabled;
  private Telemetry telemetryEngine;

  private TelemetryJob scheduledJob;

  static Path getStorageFilePath() {
    TelemetryPathManager.migrate(TELEMETRY_PRODUCT_KEY, getOldStorageFilePath());
    return TelemetryPathManager.getPath(TELEMETRY_PRODUCT_KEY);
  }

  private static Path getOldStorageFilePath() {
    return SonarLintCorePlugin.getInstance().getStateLocation().toFile().toPath().resolve(OLD_STORAGE_FILENAME);
  }

  public void optOut(boolean optOut) {
    if (telemetryEngine != null) {
      if (optOut == !telemetryEngine.enabled()) {
        return;
      }
      telemetryEngine.enable(!optOut);
      if (optOut) {
        try {
          TelemetryClientConfig clientConfig = getTelemetryClientConfig();
          telemetryEngine.getClient().optOut(clientConfig, isAnyProjectConnected());
        } catch (Exception e) {
          // Silently ignore
        }
      }
    }
  }

  public boolean enabled() {
    return enabled;
  }

  public boolean optedIn() {
    return enabled && this.telemetryEngine.enabled();
  }

  public void init() {
    if ("true".equals(System.getProperty(DISABLE_PROPERTY_KEY))) {
      this.enabled = false;
      SonarLintLogger.get().info("Telemetry disabled by system property");
      return;
    }
    try {
      this.telemetryEngine = new Telemetry(getStorageFilePath(), PRODUCT, SonarLintUtils.getPluginVersion());
      SonarLintCorePlugin.getAnalysisListenerManager().addListener(this);
      this.scheduledJob = new TelemetryJob();
      scheduledJob.schedule(TimeUnit.MINUTES.toMillis(1));
      this.enabled = true;
    } catch (Exception e) {
      // Silently ignore
      enabled = false;
    }
  }

  private class TelemetryJob extends Job {

    public TelemetryJob() {
      super("SonarLint Telemetry");
    }

    protected IStatus run(IProgressMonitor monitor) {
      schedule(TimeUnit.HOURS.toMillis(6));
      if (enabled) {
        try {
          TelemetryClientConfig clientConfig = getTelemetryClientConfig();
          telemetryEngine.getClient().tryUpload(clientConfig, isAnyProjectConnected());
        } catch (Exception e) {
          // Silently ignore
        }
      }
      return Status.OK_STATUS;
    }

  }

  public static TelemetryClientConfig getTelemetryClientConfig() {
    TelemetryClientConfig.Builder clientConfigBuilder = new TelemetryClientConfig.Builder()
      .userAgent("SonarLint");
    IProxyService proxyService = SonarLintCorePlugin.getInstance().getProxyService();
    IProxyData[] proxyDataForHost;
    try {
      proxyDataForHost = proxyService.select(new URL(Telemetry.TELEMETRY_ENDPOINT).toURI());
    } catch (MalformedURLException | URISyntaxException e) {
      // URL is a constant, should never occurs
      throw new IllegalStateException(e);
    }

    for (IProxyData data : proxyDataForHost) {
      if (data.getHost() != null) {
        clientConfigBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getHost(), data.getPort())));
        if (data.isRequiresAuthentication()) {
          clientConfigBuilder.proxyLogin(data.getUserId());
          clientConfigBuilder.proxyPassword(data.getPassword());
        }
        break;
      }
    }
    return clientConfigBuilder.build();
  }

  @Override
  public void analysisCompleted(AnalysisEvent event) {
    if (enabled) {
      telemetryEngine.getDataCollection().analysisDone();
    }
  }

  public void stop() {
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(this);
    if (scheduledJob != null) {
      scheduledJob.cancel();
      scheduledJob = null;
    }
    try {
      if (telemetryEngine != null) {
        telemetryEngine.save();
      }
    } catch (IOException e) {
      // Silently ignore
    }
  }

  private static boolean isAnyProjectConnected() {
    return ProjectsProviderUtils.allProjects().stream().anyMatch(p -> p.isOpen() && p.isBound());
  }
}
