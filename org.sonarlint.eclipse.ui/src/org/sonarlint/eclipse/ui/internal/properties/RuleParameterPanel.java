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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleParamDefinitionDto;

public class RuleParameterPanel extends Composite {
  private final Link defaultLink;
  private final RuleDefinitionDto selectedRuleMetadata;
  private final RuleConfig selectedRuleConfig;
  private Composite paramInputsContainer;

  public RuleParameterPanel(Composite parent, int style, RuleDefinitionDto selectedRuleMetadata, RuleConfig selectedRuleConfig) {
    super(parent, style);
    this.selectedRuleMetadata = selectedRuleMetadata;
    this.selectedRuleConfig = selectedRuleConfig;
    this.setLayout(new GridLayout());
    var group = new Group(this, SWT.LEFT);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    group.setText("Parameters");
    var groupLayout = new GridLayout();
    groupLayout.verticalSpacing = 0;
    groupLayout.marginHeight = 0;
    group.setLayout(groupLayout);

    defaultLink = new Link(group, SWT.NONE);
    defaultLink.setText("<a>Restore defaults</a>");
    defaultLink.setToolTipText("Restore default parameters values");
    defaultLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
    defaultLink.setEnabled(selectedRuleConfig.isActive());
    setDefaultLinkVisibility();
    var sc = new ScrolledComposite(group, SWT.H_SCROLL | SWT.V_SCROLL);
    sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    defaultLink.addListener(SWT.Selection, e -> {
      selectedRuleConfig.getParams().clear();
      paramInputsContainer.dispose();
      createParamInputs(sc);
      paramInputsContainer.requestLayout();
      setDefaultLinkVisibility();
    });

    createParamInputs(sc);
  }

  private void createParamInputs(ScrolledComposite sc) {
    paramInputsContainer = new Composite(sc, SWT.NONE);
    sc.setContent(paramInputsContainer);
    sc.setExpandVertical(true);
    sc.setExpandHorizontal(true);

    var layout = new GridLayout();
    layout.numColumns = 2;
    paramInputsContainer.setLayout(layout);

    selectedRuleMetadata.getParamsByKey().values().forEach(it -> addParamInput(paramInputsContainer, it));

    sc.setMinSize(paramInputsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  public void setDefaultLinkVisibility() {
    var allParamAreDefault = selectedRuleMetadata.getParamsByKey().values().stream()
      .allMatch(param -> !selectedRuleConfig.getParams().containsKey(param.getKey())
        || Objects.equals(param.getDefaultValue(), selectedRuleConfig.getParams().get(param.getKey())));
    defaultLink.setVisible(!allParamAreDefault);
  }

  void addParamInput(Composite parent, RuleParamDefinitionDto ruleParam) {
    var ruleParameterLabel = new Label(parent, SWT.NONE);
    var layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    ruleParameterLabel.setLayoutData(layoutData);
    ruleParameterLabel.setText(ruleParam.getName());

    var rp = ruleParam.getType();
    switch (rp) {
      case BOOLEAN:
        addCheckboxInput(parent, ruleParam);
        break;
      case INTEGER:
        addIntegerInput(parent, ruleParam);
        break;
      case TEXT:
        addTextInput(parent, ruleParam);
        break;
      case STRING:
        addStringInput(parent, ruleParam);
        break;
      case FLOAT:
        // We are not aware of rules using FLOAT, so use a simple string input
        addStringInput(parent, ruleParam);
        break;
      default:
        SonarLintLogger.get().error("Unknown rule parameter type: " + rp + " for rule " + ruleParam.getKey());
    }
  }

  private void addTextInput(Composite parent, RuleParamDefinitionDto ruleParam) {
    var ruleParameterInput = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
    var layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    ruleParameterInput.setLayoutData(layoutData);
    configureInput(ruleParam, ruleParameterInput);
  }

  private void addStringInput(Composite parent, RuleParamDefinitionDto ruleParam) {
    var ruleParameterInput = new Text(parent, SWT.SINGLE | SWT.BORDER);
    var layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
    ruleParameterInput.setLayoutData(layoutData);
    configureInput(ruleParam, ruleParameterInput);
  }

  private void configureInput(RuleParamDefinitionDto ruleParam, Text ruleParameterInput) {
    ruleParameterInput.setToolTipText(ruleParam.getDescription());
    ruleParameterInput.setText(StringUtils.trimToEmpty(selectedRuleConfig.getParams().getOrDefault(ruleParam.getKey(), ruleParam.getDefaultValue())));
    ruleParameterInput.addModifyListener(e -> {
      var text = ((Text) e.widget).getText();
      if (!StringUtils.isEmpty(text)) {
        selectedRuleConfig.getParams().put(ruleParam.getKey(), text);
      } else {
        selectedRuleConfig.getParams().remove(ruleParam.getKey());
      }
      setDefaultLinkVisibility();
    });
    ruleParameterInput.setEnabled(selectedRuleConfig.isActive());
  }

  private void addIntegerInput(Composite parent, RuleParamDefinitionDto ruleParam) {
    var ruleParameterInput = new Spinner(parent, SWT.WRAP);
    var layoutData = new GridData(SWT.FILL, SWT.NONE, false, false);
    ruleParameterInput.setToolTipText(ruleParam.getDescription());
    ruleParameterInput.setLayoutData(layoutData);
    ruleParameterInput.setMinimum(Integer.MIN_VALUE);
    ruleParameterInput.setMaximum(Integer.MAX_VALUE);
    ruleParameterInput.setSelection(asInteger(selectedRuleConfig.getParams().getOrDefault(ruleParam.getKey(), ruleParam.getDefaultValue())));
    ruleParameterInput.addModifyListener(e -> {
      selectedRuleConfig.getParams().put(ruleParam.getKey(), String.valueOf(((Spinner) e.widget).getSelection()));
      setDefaultLinkVisibility();
    });
    ruleParameterInput.setEnabled(selectedRuleConfig.isActive());
  }

  private static int asInteger(@Nullable String value) {
    if (StringUtils.isEmpty(value)) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void addCheckboxInput(Composite parent, RuleParamDefinitionDto ruleParam) {
    var ruleParameterInput = new Button(parent, SWT.CHECK);
    ruleParameterInput.setToolTipText(ruleParam.getDescription());
    ruleParameterInput.setSelection("true".equals(selectedRuleConfig.getParams().getOrDefault(ruleParam.getKey(), ruleParam.getDefaultValue())));
    ruleParameterInput.addListener(SWT.Selection, e -> {
      selectedRuleConfig.getParams().put(ruleParam.getKey(), String.valueOf(((Button) e.widget).getSelection()));
      setDefaultLinkVisibility();
    });
    ruleParameterInput.setEnabled(selectedRuleConfig.isActive());
  }
}
