/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.views;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;

public class BindingsView extends WorkbenchView {

  public BindingsView() {
    super("SonarLint Bindings");
  }

  public void updateAllProjectBindings() {
    getBindings().forEach(Binding::updateAllProjectBindings);
  }

  public boolean isBindingEmpty() {
    activate();
    try {
      new DefaultLink(cTabItem, new WithTextMatcher(
        "<a>Connect to SonarQube/SonarCloud...</a>"));
      return true;
    } catch (CoreLayerException e) {
      return false;
    }
  }

  public void removeAllBindings() {
    if (!isBindingEmpty()) {
      new DefaultTree(cTabItem).getItems().forEach(item -> {
        item.select();
        new ContextMenuItem("Delete Connection").select();
        var s = new DefaultShell("Delete Connection(s)");
        new PushButton(s, "OK").click();
      });
    }
  }

  public List<Binding> getBindings() {
    activate();
    return isBindingEmpty() ? Collections.emptyList() : new DefaultTree(cTabItem).getItems().stream().map(Binding::new).collect(Collectors.toList());
  }

  public static class Binding {

    private final TreeItem i;

    private Binding(TreeItem i) {
      this.i = i;
    }

    public String getLabel() {
      return i.getText();
    }

    @Override
    public String toString() {
      return i.getText();
    }

    public void updateAllProjectBindings() {
      i.select();
      new ContextMenuItem("Update All Project Bindings").select();
    }

  }

}
