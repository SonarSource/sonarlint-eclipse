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
package org.sonarlint.eclipse.ui.internal.rule;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.rpc.protocol.common.StandardModeDetails;

/** Rule header for the old CCT */
public class LegacyRuleHeaderPanel extends AbstractRuleHeaderPanel {
  private final Label ruleTypeIcon;
  private final Label ruleTypeLabel;
  private final Label ruleSeverityIcon;
  private final Label ruleSeverityLabel;
  private final Label ruleKeyLabel;

  private final StandardModeDetails details;

  public LegacyRuleHeaderPanel(Composite parent, StandardModeDetails details, String ruleKey) {
    super(parent, 5, ruleKey);

    ruleTypeIcon = new Label(this, SWT.NONE);
    ruleTypeLabel = new Label(this, SWT.NONE);
    ruleSeverityIcon = new Label(this, SWT.NONE);
    ruleSeverityLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
    this.details = details;
  }

  @Override
  public void updateRule() {
    var type = details.getType();
    ruleTypeIcon.setImage(SonarLintImages.getTypeImage(type));
    ruleTypeLabel.setText(clean(type.toString()));

    var severity = details.getSeverity();
    ruleSeverityIcon.setImage(SonarLintImages.getSeverityImage(severity));
    ruleSeverityLabel.setText(clean(severity.toString()));

    ruleKeyLabel.setText(ruleKey);
    layout();
  }
}
