/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.preferences;

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class AnalyzerPropertiesPreferences extends PropertyPage {
  private static final String NAME_COL = "Name";
  private static final String VALUE_COL = "Value";

  public AnalyzerPropertiesPreferences(ReferencedComposite composite) {
    super(composite, new String[] {"SonarQube", "Analyzer Properties"});
  }

  public void add(String name, String value) {
    new PushButton(referencedComposite, "New...").click();

    var shell = new DefaultShell("New property");
    var nameInput = new DefaultText(shell, 0);
    nameInput.setText(name);
    var valueInput = new DefaultText(shell, 1);
    valueInput.setText(value);

    new OkButton().click();
  }

  public List<AnalyzerProperty> getProperties() {
    var table = new DefaultTable(referencedComposite);
    return table.getItems().stream()
      .map(i -> new AnalyzerProperty(
        i.getText(table.getHeaderIndex(NAME_COL)),
        i.getText(table.getHeaderIndex(VALUE_COL))))
      .collect(Collectors.toList());
  }

  public void clear() {
    var table = new DefaultTable(referencedComposite);
    for (var item : table.getItems()) {
      table.select(table.indexOf(item));
      new PushButton(referencedComposite, "Remove").click();
    }
  }

  public static class AnalyzerProperty {
    private final String name;
    private final String value;

    public AnalyzerProperty(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }
}
