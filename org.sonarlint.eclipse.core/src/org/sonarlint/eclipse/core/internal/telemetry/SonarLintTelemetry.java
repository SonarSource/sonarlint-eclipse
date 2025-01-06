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
package org.sonarlint.eclipse.core.internal.telemetry;

import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.telemetry.TelemetryRpcService;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.AddQuickFixAppliedForRuleParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.DevNotificationsClickedParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.FixSuggestionResolvedParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.FixSuggestionStatus;

public class SonarLintTelemetry {

  private SonarLintTelemetry() {

  }

  public static String ideVersionForTelemetry() {
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

  private static TelemetryRpcService getTelemetryService() {
    return SonarLintBackendService.get().getBackend().getTelemetryService();
  }

  public static void devNotificationsClicked(String category) {
    getTelemetryService().devNotificationsClicked(new DevNotificationsClickedParams(category));
  }

  public static boolean isEnabled() {
    try {
      return getTelemetryService().getStatus().get().isEnabled();
    } catch (InterruptedException | ExecutionException e) {
      SonarLintLogger.get().debug("Unable to get telemetry status", e);
      return false;
    }
  }

  public static void optOut(boolean optOut) {
    if (optOut) {
      getTelemetryService().disableTelemetry();
    } else {
      getTelemetryService().enableTelemetry();
    }
  }

  public static void addQuickFixAppliedForRule(String ruleKey) {
    getTelemetryService().addQuickFixAppliedForRule(new AddQuickFixAppliedForRuleParams(ruleKey));
  }

  public static void taintVulnerabilitiesInvestigatedRemotely() {
    getTelemetryService().taintVulnerabilitiesInvestigatedRemotely();
  }

  public static void taintVulnerabilitiesInvestigatedLocally() {
    getTelemetryService().taintVulnerabilitiesInvestigatedLocally();
  }

  public static void addedManualBindings() {
    getTelemetryService().addedManualBindings();
  }

  public static void addedImportedBindings() {
    getTelemetryService().addedImportedBindings();
  }

  public static void addedAutomaticBindings() {
    getTelemetryService().addedAutomaticBindings();
  }

  public static void acceptFixSuggestion(String id, int changeIndex) {
    getTelemetryService().fixSuggestionResolved(
      new FixSuggestionResolvedParams(id, FixSuggestionStatus.ACCEPTED, changeIndex));
  }

  public static void declineFixSuggestion(String id, int changeIndex) {
    getTelemetryService().fixSuggestionResolved(
      new FixSuggestionResolvedParams(id, FixSuggestionStatus.DECLINED, changeIndex));
  }
}
