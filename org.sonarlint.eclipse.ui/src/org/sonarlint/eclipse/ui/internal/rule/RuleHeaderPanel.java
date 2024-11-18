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
package org.sonarlint.eclipse.ui.internal.rule;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.client.utils.ImpactSeverity;
import org.sonarsource.sonarlint.core.client.utils.SoftwareQuality;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.ImpactDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.CleanCodeAttribute;

/** Rule header for the new CCT */
public class RuleHeaderPanel extends AbstractRuleHeaderPanel {
  private final Label ruleCleanCodeAttributeLabel;
  private final SoftwareQualityImpactPanel firstSoftwareQualityImpact;
  private final SoftwareQualityImpactPanel secondSoftwareQualityImpact;
  private final SoftwareQualityImpactPanel thirdSoftwareQualityImpact;
  private final Label ruleKeyLabel;
  private final CleanCodeAttribute cleanCodeAttribute;
  private final List<ImpactDto> impacts;

  public RuleHeaderPanel(Composite parent, CleanCodeAttribute cleanCodeAttribute, List<ImpactDto> impacts, String ruleKey) {
    super(parent, 5, ruleKey);

    ruleCleanCodeAttributeLabel = new Label(this, SWT.NONE);
    firstSoftwareQualityImpact = new SoftwareQualityImpactPanel(this, SWT.NONE);
    secondSoftwareQualityImpact = new SoftwareQualityImpactPanel(this, SWT.NONE);
    thirdSoftwareQualityImpact = new SoftwareQualityImpactPanel(this, SWT.LEFT);
    ruleKeyLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
    this.cleanCodeAttribute = cleanCodeAttribute;
    this.impacts = impacts;
  }

  @Override
  public void updateRule() {
    var ccaWithLabel = org.sonarsource.sonarlint.core.client.utils.CleanCodeAttribute.fromDto(cleanCodeAttribute);
    ruleCleanCodeAttributeLabel.setText(
      clean(ccaWithLabel.getCategory().getLabel()) + " | " + clean(ccaWithLabel.getLabel()));
    ruleCleanCodeAttributeLabel.setToolTipText(
      "Clean Code attributes are characteristics code needs to have to be considered clean.");

    firstSoftwareQualityImpact.updateImpact(impacts.get(0));
    if (impacts.size() > 1) {
      secondSoftwareQualityImpact.updateImpact(impacts.get(1));
      if (impacts.size() > 2) {
        thirdSoftwareQualityImpact.updateImpact(impacts.get(2));
      }
    }

    ruleKeyLabel.setText(ruleKey);
    layout();
  }

  private static class SoftwareQualityImpactPanel extends Composite {
    private final Label softwareQualityLabel;
    private final Label impactSeverityIcon;

    SoftwareQualityImpactPanel(Composite parent, int style) {
      super(parent, style);
      setLayout(new GridLayout(2, false));

      softwareQualityLabel = new Label(this, SWT.NONE);
      impactSeverityIcon = new Label(this, SWT.LEFT);
    }

    public void updateImpact(ImpactDto impact) {
      var impactSeverityWithLabel = ImpactSeverity.fromDto(impact.getImpactSeverity());
      var sqWithLabel = SoftwareQuality.fromDto(impact.getSoftwareQuality());
      var tooltip = "Issues found for this rule will have a " + impactSeverityWithLabel.getLabel() +
        " impact on the " + sqWithLabel.getLabel() + " of your software.";

      softwareQualityLabel.setText(sqWithLabel.getLabel());
      softwareQualityLabel.setToolTipText(tooltip);
      impactSeverityIcon.setImage(SonarLintImages.getImpactImage(impact.getImpactSeverity()));
      impactSeverityIcon.setToolTipText(tooltip);
    }
  }
}
