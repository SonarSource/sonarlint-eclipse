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

import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.AbstractRuleDto;
import org.sonarsource.sonarlint.core.commons.ImpactSeverity;
import org.sonarsource.sonarlint.core.commons.SoftwareQuality;

/** Rule header for the new CCT */
public class RuleHeaderPanel extends AbstractRuleHeaderPanel {
  private final Label ruleCleanCodeAttributeLabel;
  private final SoftwareQualityImpactPanel firstSoftwareQualityImpact;
  private final SoftwareQualityImpactPanel secondSoftwareQualityImpact;
  private final SoftwareQualityImpactPanel thirdSoftwareQualityImpact;
  private final Label ruleKeyLabel;

  public RuleHeaderPanel(Composite parent) {
    super(parent, 5);
    
    ruleCleanCodeAttributeLabel = new Label(this, SWT.NONE);
    firstSoftwareQualityImpact= new SoftwareQualityImpactPanel(this, SWT.NONE);
    secondSoftwareQualityImpact = new SoftwareQualityImpactPanel(this, SWT.NONE);
    thirdSoftwareQualityImpact = new SoftwareQualityImpactPanel(this, SWT.LEFT);
    ruleKeyLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
  }
  
  @Override
  public void updateRule(AbstractRuleDto ruleInformation) {
    /** INFO: We assume that the Optional#isPresent() check was already done */
    var attribute = ruleInformation.getCleanCodeAttribute().get();
    ruleCleanCodeAttributeLabel.setText(
      clean(attribute.getAttributeCategory().getIssueLabel()) + " | " + clean(attribute.getIssueLabel()));
    ruleCleanCodeAttributeLabel.setToolTipText(
      "Clean Code attributes are characteristics code needs to have to be considered clean.");
    
    var impacts = ruleInformation.getDefaultImpacts();
    var keys = new ArrayList<SoftwareQuality>(impacts.keySet());
    
    firstSoftwareQualityImpact.updateRule(keys.get(0), impacts.get(keys.get(0)));
    if (keys.size() > 1) {
      secondSoftwareQualityImpact.updateRule(keys.get(1), impacts.get(keys.get(1)));
      if (keys.size() > 2) {
        thirdSoftwareQualityImpact.updateRule(keys.get(2), impacts.get(keys.get(2)));
      }
    }
    
    ruleKeyLabel.setText(ruleInformation.getKey());
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
    
    public void updateRule(SoftwareQuality quality, ImpactSeverity impact) {
      var tooltip = createImpactToolTip(quality, impact);
      
      softwareQualityLabel.setText(quality.getDisplayLabel());
      softwareQualityLabel.setToolTipText(tooltip);
      impactSeverityIcon.setImage(SonarLintImages.getImpactImage(impact));
      impactSeverityIcon.setToolTipText(tooltip);
    }
    
    private static String createImpactToolTip(SoftwareQuality quality, ImpactSeverity impact) {
      return "Issues found for this rule will have a " + impact.getDisplayLabel() +
        " impact on the " + quality.getDisplayLabel() + " of your software.";
    }
  }
}
