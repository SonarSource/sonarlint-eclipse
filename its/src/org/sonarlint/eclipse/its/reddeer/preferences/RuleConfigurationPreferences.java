/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.preferences;

import java.util.List;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.api.Spinner;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.spinner.DefaultSpinner;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;

public class RuleConfigurationPreferences extends PropertyPage {

  public RuleConfigurationPreferences(WorkbenchPreferenceDialog preferenceDialog) {
    super(preferenceDialog, "SonarLint", "Rules Configuration");
  }

  public void add(String exclusion) {
    new PushButton(referencedComposite, "New...").click();

    Shell shell = new DefaultShell("Create Exclusion");

    Text text = new DefaultText(shell);
    text.setText(exclusion);

    new OkButton(shell).click();
  }

  public void filter(String ruleFilter) {
    new DefaultText(this, 1).setText(ruleFilter);
  }

  public List<TreeItem> getItems() {
    return new DefaultTree(this, 1).getItems();
  }

  public TreeItem getItem(String... itemPath) {
    return new DefaultTree(this, 1).getItem(itemPath);
  }

  public void setRuleParameter(int parameterValue) {
    getRuleParamSpinner().setValue(parameterValue);
  }

  public Spinner getRuleParamSpinner() {
    return new DefaultSpinner();
  }

  public void ok() {
    ((WorkbenchPreferenceDialog) referencedComposite).ok();
  }
}
