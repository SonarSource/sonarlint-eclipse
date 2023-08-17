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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.AbstractRuleDto;
import org.sonarsource.sonarlint.core.commons.SoftwareQuality;

/** Rule header for the new CCT */
public class RuleHeaderPanel extends AbstractRuleHeaderPanel {
  private final Label ruleCleanCodeAttributeLabel;
  private final Label firstImpactSeverityIcon;
  private final Label firstSoftwareQualityLabel;
  private final Label secondImpactSeverityIcon;
  private final Label secondSoftwareQualityLabel;
  private final Label thirdImpactSeverityIcon;
  private final Label thirdSoftwareQualityLabel;
  private final Label ruleKeyLabel;

  public RuleHeaderPanel(Composite parent) {
    super(parent, 8);
    
    ruleCleanCodeAttributeLabel = new Label(this, SWT.NONE);
    firstImpactSeverityIcon = new Label(this, SWT.NONE);
    firstSoftwareQualityLabel = new Label(this, SWT.NONE);
    secondImpactSeverityIcon = new Label(this, SWT.NONE);
    secondSoftwareQualityLabel = new Label(this, SWT.NONE);
    thirdImpactSeverityIcon = new Label(this, SWT.NONE);
    thirdSoftwareQualityLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
  }
  
  @Override
  public void updateRule(AbstractRuleDto ruleInformation) {
    /** INFO: We assume that the Optional#isPresent() check was already done */
    var attribute = ruleInformation.getCleanCodeAttribute().get();
    ruleCleanCodeAttributeLabel.setText(
      clean(attribute.getAttributeCategory().getIssueLabel()) + " | " + clean(attribute.getIssueLabel()));
    
    var impacts = ruleInformation.getDefaultImpacts();
    var keys = new ArrayList<SoftwareQuality>(impacts.keySet());
    firstImpactSeverityIcon.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(0))));
    firstSoftwareQualityLabel.setText(clean(keys.get(0).getDisplayLabel()));
    
    if (keys.size() > 1) {
      secondImpactSeverityIcon.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(1))));
      secondSoftwareQualityLabel.setText(clean(impacts.get(keys.get(1)).getDisplayLabel()));
      
      if (keys.size() > 2) {
        thirdImpactSeverityIcon.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(2))));
        thirdSoftwareQualityLabel.setText(clean(impacts.get(keys.get(2)).getDisplayLabel()));
      }
    }
    
    ruleKeyLabel.setText(ruleInformation.getKey());
    layout();
  }
}
