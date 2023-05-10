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

import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;

/** Rule header containing all information excluding the title and description */
public class RuleHeaderPanel extends Composite {
  private final Label ruleTypeIcon;
  private final CopyableLabel ruleTypeLabel;
  private final Label ruleSeverityIcon;
  private final CopyableLabel ruleSeverityLabel;
  private final CopyableLabel ruleKeyLabel;

  public RuleHeaderPanel(Composite parent) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(5, false));

    ruleTypeIcon = new Label(this, SWT.NONE);

    ruleTypeLabel = new CopyableLabel(this);

    ruleSeverityIcon = new Label(this, SWT.NONE);

    ruleSeverityLabel = new CopyableLabel(this);

    var ruleKeyFiller = new Composite(this, SWT.NONE);
    ruleKeyFiller.setLayout(new GridLayout(1, false));
    ruleKeyFiller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    ruleKeyLabel = new CopyableLabel(ruleKeyFiller);
    ruleKeyLabel.setLayoutData(new GridData(SWT.END, SWT.FILL, true, true));
  }

  public void clearRule() {
    ruleTypeIcon.setImage(null);
    ruleTypeLabel.setText("");
    ruleKeyLabel.setText("");
    ruleSeverityIcon.setImage(null);
    ruleSeverityLabel.setText("");
  }

  /** Updating the panel requires each element to adjust to the grid again */
  public void updateRule(String ruleKey, RuleType type, IssueSeverity severity) {
    ruleTypeIcon.setImage(SonarLintImages.getTypeImage(type));
    ruleTypeIcon.requestLayout();
    ruleTypeLabel.setText(clean(type.toString()));
    ruleTypeLabel.requestLayout();
    ruleKeyLabel.setText(ruleKey);
    ruleKeyLabel.requestLayout();
    ruleSeverityIcon.setImage(SonarLintImages.getSeverityImage(severity));
    ruleSeverityIcon.requestLayout();
    ruleSeverityLabel.setText(clean(severity.toString()));
    ruleSeverityLabel.requestLayout();
  }

  private static String clean(@Nullable String txt) {
    if (txt == null) {
      return "";
    }
    return StringUtils.capitalize(txt.toLowerCase(Locale.ENGLISH).replace("_", " "));
  }

}
