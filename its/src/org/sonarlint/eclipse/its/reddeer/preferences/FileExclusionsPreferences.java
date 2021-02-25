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
import java.util.stream.Collectors;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.api.Table;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class FileExclusionsPreferences extends PropertyPage {

  private static final String VALUE_COL = "Value";

  public FileExclusionsPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, new String[] {"SonarLint", "File Exclusions"});
  }

  public void add(String exclusion) {
    new PushButton(referencedComposite, "New...").click();

    Shell shell = new DefaultShell("Create Exclusion");

    Text text = new DefaultText(shell);
    text.setText(exclusion);

    new OkButton(shell).click();
  }

  public List<String> getExclusions() {
    Table table = new DefaultTable(this);
    return table.getItems().stream().map(i -> i.getText(table.getHeaderIndex(VALUE_COL))).collect(Collectors.toList());
  }

  public void remove(String exclusion) {
    Table table = new DefaultTable(referencedComposite);

    for (TableItem item : table.getItems()) {
      if (exclusion.equals(item.getText(table.getHeaderIndex(VALUE_COL)))) {
        table.select(table.indexOf(item));
        new PushButton(referencedComposite, "Remove").click();
        return;
      }
    }
    throw new IllegalArgumentException("Unable to find exclusion with value: " + exclusion);

  }

}
