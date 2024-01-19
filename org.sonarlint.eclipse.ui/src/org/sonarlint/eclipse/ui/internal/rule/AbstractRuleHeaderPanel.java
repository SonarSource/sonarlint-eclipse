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

import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.AbstractRuleDto;

public abstract class AbstractRuleHeaderPanel extends Composite {
  protected AbstractRuleHeaderPanel(Composite parent, int numColumns) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(numColumns, false));
  }
  
  public abstract void updateRule(AbstractRuleDto ruleInformation);
  
  protected static String clean(@Nullable String txt) {
    return txt == null
      ? ""
      : StringUtils.capitalize(txt.toLowerCase(Locale.ENGLISH).replace("_", " "));
  }
}
