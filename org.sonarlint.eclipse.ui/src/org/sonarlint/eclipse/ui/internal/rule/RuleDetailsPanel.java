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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;

/**
 *  Panel containing the rule title, details and description
 *
 *  | Rule title                                                         |  -> StyledText
 *  | Type icon | Type label | Severity icon | Severity label | Rule key |  -> RuleHeaderPanel
 *  | Rule description                                                   |  -> SonarLintRuleBrowser
 */
public class RuleDetailsPanel extends Composite {

  private final StyledText ruleNameLabel;
  private final RuleHeaderPanel ruleHeaderPanel;
  private final SonarLintRuleBrowser description;
  private final Font nameLabelFont;

  public RuleDetailsPanel(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);

    var layout = new GridLayout(1, false);
    setLayout(layout);

    ruleNameLabel = new StyledText(this, SWT.LEFT);
    ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    nameLabelFont = FontDescriptor.createFrom(ruleNameLabel.getFont())
      .setStyle(SWT.BOLD)
      .increaseHeight(3)
      .createFont(ruleNameLabel.getDisplay());
    ruleNameLabel.setFont(nameLabelFont);
    ruleNameLabel.setEditable(false);
    ruleNameLabel.setCaret(null);
    ruleNameLabel.setBackground(getBackground());

    ruleHeaderPanel = new RuleHeaderPanel(this);
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

    description = new SonarLintRuleBrowser(this, useEditorFontSize);
    description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    // TODO params
  }

  @Override
  public void dispose() {
    nameLabelFont.dispose();
    super.dispose();
  }

  public void updateRule(@Nullable RuleDetails ruleDetails) {
    if (ruleDetails != null) {
      ruleNameLabel.setText(ruleDetails.getName());
      ruleHeaderPanel.update(ruleDetails.getKey(), ruleDetails.getType(), ruleDetails.getDefaultSeverity());
    } else {
      ruleNameLabel.setText("No rules selected");
      ruleHeaderPanel.clear();
    }
    description.updateRule(ruleDetails);
    // requestLayout();
  }

}
