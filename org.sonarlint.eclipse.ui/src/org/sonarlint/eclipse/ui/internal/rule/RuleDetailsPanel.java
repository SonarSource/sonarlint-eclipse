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
package org.sonarlint.eclipse.ui.internal.rule;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetEffectiveRuleDetailsResponse;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetStandaloneRuleDescriptionResponse;

/**
 *  Panel containing the rule title, details and description
 *
 *  | Rule title                                                         |  -> StyledText
 *  | Type icon | Type label | Severity icon | Severity label | Rule key |  -> RuleHeaderPanel
 *  | Rule description                                                   |  -> RuleDescriptionPanel
 */
public class RuleDetailsPanel extends Composite {

  private final CopyableLabel ruleNameLabel;
  private final RuleHeaderPanel ruleHeaderPanel;
  @Nullable
  private RuleDescriptionPanel ruleDescriptionPanel;
  private final Font nameLabelFont;
  private final boolean useEditorFontSize;

  public RuleDetailsPanel(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    this.useEditorFontSize = useEditorFontSize;
    setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    var layout = new GridLayout(1, false);
    setLayout(layout);

    ruleNameLabel = new CopyableLabel(this);
    ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    nameLabelFont = FontDescriptor.createFrom(ruleNameLabel.getFont())
      .setStyle(SWT.BOLD)
      .increaseHeight(3)
      .createFont(ruleNameLabel.getDisplay());
    ruleNameLabel.setFont(nameLabelFont);

    ruleHeaderPanel = new RuleHeaderPanel(this);
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

    // TODO params
  }

  @Override
  public void dispose() {
    nameLabelFont.dispose();
    super.dispose();
  }

  public void updateRule(GetStandaloneRuleDescriptionResponse getStandaloneRuleDescriptionResponse) {
    var ruleDefinition = getStandaloneRuleDescriptionResponse.getRuleDefinition();

    ruleNameLabel.setText(ruleDefinition.getName());
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.updateRule(ruleDefinition.getKey(), ruleDefinition.getType(), ruleDefinition.getDefaultSeverity());

    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
    ruleDescriptionPanel = new RuleDescriptionPanel(this, this.useEditorFontSize);
    ruleDescriptionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    ruleDescriptionPanel.updateRule(getStandaloneRuleDescriptionResponse.getDescription());
    requestLayout();
  }

  public void updateRule(GetEffectiveRuleDetailsResponse getEffectiveRuleDetailsResponse) {
    var details = getEffectiveRuleDetailsResponse.details();

    ruleNameLabel.setText(details.getName());
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.updateRule(details.getKey(), details.getType(), details.getSeverity());

    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
    ruleDescriptionPanel = new RuleDescriptionPanel(this, this.useEditorFontSize);
    ruleDescriptionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    ruleDescriptionPanel.updateRule(details.getDescription());
    requestLayout();
  }

  public void clearRule() {
    ruleNameLabel.setText("No rule selected");
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.clearRule();

    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
  }
}
