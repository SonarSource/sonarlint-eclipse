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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.Collection;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.properties.RulesConfigurationPart.ExclusionsAndInclusions;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class RulesConfigurationPage extends PropertyPage implements IWorkbenchPreferencePage {

  private RulesConfigurationPart rulesConfigurationPart;

  public RulesConfigurationPage() {
    setTitle("Rules Configuration");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Configure rules used for SonarLint analysis. When a project is connected to a SonarQube/SonarCloud server, configuration from the server applies.");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite pageComponent = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    pageComponent.setLayout(layout);

    rulesConfigurationPart = new RulesConfigurationPart(loadLanguages(), loadRuleDetails(), getExcludedRules(), getIncludedRules());
    rulesConfigurationPart.createControls(pageComponent);
    Dialog.applyDialogFont(pageComponent);
    return pageComponent;
  }

  private static Map<String, String> loadLanguages() {
    return SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().getAllLanguagesNameByKey();
  }

  private static Collection<RuleDetails> loadRuleDetails() {
    return SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().getAllRuleDetails();
  }

  private static Collection<RuleKey> getExcludedRules() {
    return PreferencesUtils.getExcludedRules();
  }

  private static Collection<RuleKey> getIncludedRules() {
    return PreferencesUtils.getIncludedRules();
  }

  @Override
  public boolean performOk() {
    ExclusionsAndInclusions config = rulesConfigurationPart.computeExclusionsAndInclusions();
    PreferencesUtils.setExcludedRules(config.excluded());
    PreferencesUtils.setIncludedRules(config.included());
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STANDALONE_CONFIG_CHANGE);
    return true;
  }

  @Override
  protected void performDefaults() {
    rulesConfigurationPart.resetToDefaults();
  }
}
