/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.MessageDialogUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleDefinitionDto;

public class RulesConfigurationPage extends PropertyPage implements IWorkbenchPreferencePage {

  public static final String RULES_CONFIGURATION_ID = "org.sonarlint.eclipse.ui.properties.RulesConfigurationPage";

  private RulesConfigurationPart rulesConfigurationPart;
  private Collection<RuleConfig> initialRuleConfigs;

  public RulesConfigurationPage() {
    setTitle("Rules Configuration");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Configure rules used for SonarLint analysis for projects not in connected mode.");
  }

  @Override
  protected Control createContents(Composite parent) {
    var pageComponent = new Composite(parent, SWT.NONE);
    var layout = new GridLayout();
    layout.marginWidth = 0;
    pageComponent.setLayout(layout);
    
    var label = new Link(pageComponent, SWT.NONE);
    label.setText("When a project is connected to <a>SonarQube/SonarCloud</a>, "
      + "configuration from the server applies.");
    label.addListener(SWT.Selection,
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.CONNECTED_MODE_LINK, e.display)
    );

    initialRuleConfigs = SonarLintGlobalConfiguration.readRulesConfig();
    rulesConfigurationPart = new RulesConfigurationPart(() -> loadRuleDetails(), initialRuleConfigs);
    rulesConfigurationPart.createControls(pageComponent);
    Dialog.applyDialogFont(pageComponent);
    return pageComponent;
  }

  private static List<RuleDefinitionDto> loadRuleDetails() {
    try {
      return new ArrayList<>(SonarLintBackendService.get().getStandaloneRules().get().getRulesByKey().values());
    } catch (Exception err) {
      SonarLintLogger.get().error("Loading all standalone rules for the configuration page failed", err);
    }

    return Collections.emptyList();
  }

  @Override
  public boolean performOk() {
    var newRuleConfigs = rulesConfigurationPart.computeRulesConfig();
    SonarLintGlobalConfiguration.saveRulesConfig(newRuleConfigs);
    if (!newRuleConfigs.equals(initialRuleConfigs)) {
      initialRuleConfigs = newRuleConfigs;
      AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STANDALONE_CONFIG_CHANGE);
      
      if (!SonarLintGlobalConfiguration.ignoreEnhancedFeatureNotifications()) {
        MessageDialogUtils.enhancedWithConnectedModeInformation("Are you working in a team?",
          "When using Connected Mode you can benefit from having the rule configuration synchronized to all "
          + "developers in your team instead of everyone having to configure it locally!");
      }
    }
    return true;
  }

  @Override
  protected void performDefaults() {
    rulesConfigurationPart.resetToDefaults();
  }
}
