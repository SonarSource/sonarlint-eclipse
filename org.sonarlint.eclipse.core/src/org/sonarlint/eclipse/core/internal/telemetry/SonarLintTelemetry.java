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
package org.sonarlint.eclipse.core.internal.telemetry;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.http.SonarLintHttpClientOkHttpImpl;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.telemetry.InternalDebug;
import org.sonarsource.sonarlint.core.telemetry.TelemetryClientAttributesProvider;
import org.sonarsource.sonarlint.core.telemetry.TelemetryHttpClient;
import org.sonarsource.sonarlint.core.telemetry.TelemetryManager;
import org.sonarsource.sonarlint.core.telemetry.TelemetryPathManager;

public class SonarLintTelemetry {
  private static final String TELEMETRY_PRODUCT_KEY = "eclipse";
  private static final String PRODUCT = "SonarLint Eclipse";
  private static final String OLD_STORAGE_FILENAME = "sonarlint_usage";
  public static final String DISABLE_PROPERTY_KEY = "sonarlint.telemetry.disabled";

  private TelemetryManager telemetry;

  @Nullable
  private TelemetryJob scheduledJob;

  static Path getStorageFilePath() {
    TelemetryPathManager.migrate(TELEMETRY_PRODUCT_KEY, getOldStorageFilePath());
    return TelemetryPathManager.getPath(TELEMETRY_PRODUCT_KEY);
  }

  private static Path getOldStorageFilePath() {
    return SonarLintCorePlugin.getInstance().getStateLocation().toFile().toPath().resolve(OLD_STORAGE_FILENAME);
  }

  public static boolean shouldBeActivated() {
    return !"true".equals(System.getProperty(DISABLE_PROPERTY_KEY));
  }

  public void optOut(boolean optOut) {
    if (telemetry != null) {
      if (optOut) {
        if (telemetry.isEnabled()) {
          telemetry.disable();
        }
      } else {
        if (!telemetry.isEnabled()) {
          telemetry.enable();
        }
      }
    }
  }

  public boolean enabled() {
    return telemetry != null && telemetry.isEnabled();
  }

  public void init() {
    try {
      var clientWithProxy = SonarLintUtils.withProxy(TelemetryHttpClient.TELEMETRY_ENDPOINT, SonarLintCorePlugin.getOkHttpClient());
      var client = new TelemetryHttpClient(PRODUCT, SonarLintUtils.getPluginVersion(), ideVersionForTelemetry(), System.getProperty("osgi.os"), System.getProperty("osgi.arch"),
        new SonarLintHttpClientOkHttpImpl(clientWithProxy.build()));
      this.telemetry = newTelemetryManager(getStorageFilePath(), client);
      this.scheduledJob = new TelemetryJob();
      scheduledJob.schedule(TimeUnit.MINUTES.toMillis(1));
    } catch (Exception e) {
      if (InternalDebug.isEnabled()) {
        SonarLintLogger.get().error("Failed during periodic telemetry job", e);
      }
    }
  }

  private static String ideVersionForTelemetry() {
    var sb = new StringBuilder();
    var iProduct = Platform.getProduct();
    if (iProduct != null) {
      sb.append(iProduct.getName());
    } else {
      sb.append("Unknown");
    }
    BundleUtils.getInstalledBundle("org.eclipse.platform")
      .ifPresent(platformBundle -> {
        sb.append(" ");
        sb.append(platformBundle.getVersion());
      });
    return sb.toString();
  }

  // visible for testing
  public TelemetryManager newTelemetryManager(Path path, TelemetryHttpClient client) {
    return new TelemetryManager(path, client, new EclipseTelemetryAttributesProvider());
  }

  public static class EclipseTelemetryAttributesProvider implements TelemetryClientAttributesProvider {

    @Override
    public boolean usesConnectedMode() {
      return isAnyOpenProjectBound();
    }

    @Override
    public boolean useSonarCloud() {
      return isAnyOpenProjectBoundToSonarCloud();
    }

    @Override
    public Optional<String> nodeVersion() {
      return Optional.ofNullable(getNodeJsVersion());
    }

    @Override
    public boolean devNotificationsDisabled() {
      return SonarLintCorePlugin.getServersManager().getServers().stream().anyMatch(IConnectedEngineFacade::areNotificationsDisabled);
    }

    @Override
    public Set<String> getNonDefaultEnabledRules() {
      var ruleKeys = SonarLintGlobalConfiguration.readRulesConfig().stream()
        .filter(RuleConfig::isActive)
        .map(RuleConfig::getKey)
        .collect(Collectors.toSet());
      // the set could contain rules enabled by default but with a parameter change
      ruleKeys.removeAll(defaultEnabledRuleKeys());
      return ruleKeys;
    }

    @Override
    public Set<String> getDefaultDisabledRules() {
      return SonarLintGlobalConfiguration.readRulesConfig().stream()
        .filter(rule -> !rule.isActive())
        .map(RuleConfig::getKey)
        .collect(Collectors.toSet());
    }

    private static Set<String> defaultEnabledRuleKeys() {
      return SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade()
        .getAllRuleDetails().stream()
        .filter(StandaloneRuleDetails::isActiveByDefault)
        .map(StandaloneRuleDetails::getKey)
        .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Object> additionalAttributes() {
      return Collections.emptyMap();
    }
  }

  private class TelemetryJob extends Job {

    public TelemetryJob() {
      super("SonarLint Telemetry");
      setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      schedule(TimeUnit.HOURS.toMillis(6));
      upload();
      return Status.OK_STATUS;
    }

  }

  // visible for testing
  public void upload() {
    if (enabled()) {
      telemetry.uploadLazily();
    }
  }

  public void analysisDoneOnMultipleFiles() {
    if (enabled()) {
      telemetry.analysisDoneOnMultipleFiles();
    }
  }

  public void analysisDoneOnSingleFile(@Nullable Language language, int time) {
    if (enabled()) {
      telemetry.analysisDoneOnSingleLanguage(language, time);
    }
  }

  public void devNotificationsReceived(String eventType) {
    if (enabled()) {
      telemetry.devNotificationsReceived(eventType);
    }
  }

  public void devNotificationsClicked(String eventType) {
    if (enabled()) {
      telemetry.devNotificationsClicked(eventType);
    }
  }

  public void showHotspotRequestReceived() {
    if (enabled()) {
      telemetry.showHotspotRequestReceived();
    }
  }

  public void taintVulnerabilitiesInvestigatedLocally() {
    if (enabled()) {
      telemetry.taintVulnerabilitiesInvestigatedLocally();
    }
  }

  public void taintVulnerabilitiesInvestigatedRemotely() {
    if (enabled()) {
      telemetry.taintVulnerabilitiesInvestigatedRemotely();
    }
  }

  public void addReportedRules(Set<String> ruleKeys) {
    if (enabled()) {
      telemetry.addReportedRules(ruleKeys);
    }
  }

  public void addQuickFixAppliedForRule(String ruleKey) {
    if (enabled()) {
      telemetry.addQuickFixAppliedForRule(ruleKey);
    }
  }

  public void stop() {
    if (scheduledJob != null) {
      scheduledJob.cancel();
      scheduledJob = null;
    }
    if (enabled()) {
      telemetry.stop();
    }
  }

  // visible for testing
  public Job getScheduledJob() {
    return scheduledJob;
  }

  public static boolean isAnyOpenProjectBound() {
    return ProjectsProviderUtils.allProjects().stream()
      .anyMatch(p -> p.isOpen() && SonarLintCorePlugin.loadConfig(p).isBound());
  }

  public static boolean isAnyOpenProjectBoundToSonarCloud() {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(p -> p.isOpen() && SonarLintCorePlugin.loadConfig(p).isBound())
      .map(SonarLintCorePlugin.getServersManager()::resolveBinding)
      .flatMap(Optional::stream)
      .map(ResolvedBinding::getEngineFacade)
      .anyMatch(IConnectedEngineFacade::isSonarCloud);
  }

  @Nullable
  public static String getNodeJsVersion() {
    var v = SonarLintCorePlugin.getNodeJsManager().getNodeJsVersion();
    return v != null ? v.toString() : null;
  }

}
