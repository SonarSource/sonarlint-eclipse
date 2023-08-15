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
import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.AbstractRuleDto;
import org.sonarsource.sonarlint.core.commons.SoftwareQuality;

/**
 *  Rule header containing all information excluding the title and description:
 *  
 *  Old Clean Code Taxonomy:
 *  - rule type                           [label 1 & 2]
 *  - rule severity                       [label 3 & 4]
 *  
 *  New Clean Code Taxonomy:
 *  - clean code attribute                [label 1]
 *  - software quality impact             [label 2 & 3]
 *  - software quality impact (optional)  [label 4 & 5]
 *  - software quality impact (optional)  [label 6 & 7]
 *  
 *  The rule key is shown in the old and new Clean Code Taxonomy!
 */
public class RuleHeaderPanel extends Composite {
  private final Label label1;
  private final Label label2;
  private final Label label3;
  private final Label label4;
  private final Label label5;
  private final Label label6;
  private final Label label7;
  private final Label ruleKeyLabel;

  public RuleHeaderPanel(Composite parent) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(8, false));
    
    label1 = new Label(this, SWT.NONE);
    label2 = new Label(this, SWT.NONE);
    label3 = new Label(this, SWT.NONE);
    label4 = new Label(this, SWT.NONE);
    label5 = new Label(this, SWT.NONE);
    label6 = new Label(this, SWT.NONE);
    label7 = new Label(this, SWT.LEFT);
    ruleKeyLabel = new Label(this, SWT.LEFT);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
  }

  public void clearRule() {
    label1.setImage(null);
    label1.setText("");
    label2.setImage(null);
    label2.setText("");
    label3.setImage(null);
    label3.setText("");
    label4.setImage(null);
    label4.setText("");
    label5.setText("");
    label6.setImage(null);
    label7.setText("");
    ruleKeyLabel.setText("");
    layout();
  }
  
  /** Updating the panel requires each element to adjust to the grid again */
  public void updateRule(AbstractRuleDto ruleInformation) {
    clearRule();
    
    var attributeOptional = ruleInformation.getCleanCodeAttribute();
    var impacts = ruleInformation.getDefaultImpacts();
    if (attributeOptional.isEmpty() || impacts.isEmpty()) {
      // old CCT
      var type = ruleInformation.getType();
      label1.setImage(SonarLintImages.getTypeImage(type));
      label2.setText(clean(type.toString()));
      
      var severity = ruleInformation.getSeverity();
      label3.setImage(SonarLintImages.getSeverityImage(severity));
      label4.setText(clean(severity.toString()));
    } else {
      // new CCT
      var attribute = attributeOptional.get();
      label1.setText(
        clean(attribute.getAttributeCategory().getIssueLabel()) + " | " + clean(attribute.getIssueLabel()));
      
      var keys = new ArrayList<SoftwareQuality>(impacts.keySet());
      label2.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(0))));
      label3.setText(clean(keys.get(0).getDisplayLabel()));
      
      if (keys.size() > 1) {
        label4.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(1))));
        label5.setText(clean(impacts.get(keys.get(1)).getDisplayLabel()));
        
        if (keys.size() > 2) {
          label6.setImage(SonarLintImages.getImpactImage(impacts.get(keys.get(2))));
          label7.setText(clean(impacts.get(keys.get(2)).getDisplayLabel()));
        }
      }
    }
    
    ruleKeyLabel.setText(ruleInformation.getKey());
    layout();
  }

  private static String clean(@Nullable String txt) {
    if (txt == null) {
      return "";
    }
    return StringUtils.capitalize(txt.toLowerCase(Locale.ENGLISH).replace("_", " "));
  }

}
