/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarlint.eclipse.its.shared.reddeer.views;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.core.lookup.MenuItemLookup;
import org.eclipse.reddeer.core.matcher.WithMnemonicTextMatchers;
import org.eclipse.reddeer.swt.impl.menu.AbstractMenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

/** Selects items from the rendered view menu, including declarative contributions. */
public class RenderedViewMenuItem extends AbstractMenuItem {

  public RenderedViewMenuItem(String... path) {
    super(MenuItemLookup.getInstance().lookFor(openViewMenu(), new WithMnemonicTextMatchers(path).getMatchers()));
  }

  private static Menu openViewMenu() {
    var menu = new AtomicReference<Menu>();
    Display.syncExec(() -> {
      var display = PlatformUI.getWorkbench().getDisplay();
      Listener listener = event -> {
        if (event.widget instanceof Menu && menu.compareAndSet(null, (Menu) event.widget)) {
          var capturedMenu = (Menu) event.widget;
          // The command waits in a nested event loop until the popup closes.
          display.asyncExec(() -> {
            if (!capturedMenu.isDisposed()) {
              capturedMenu.setVisible(false);
            }
          });
        }
      };
      display.addFilter(SWT.Show, listener);
      try {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        var handlerService = window.getService(IHandlerService.class);
        handlerService.executeCommand("org.eclipse.ui.window.showViewMenu", null);
      } catch (org.eclipse.core.commands.ExecutionException | org.eclipse.core.commands.NotEnabledException
        | org.eclipse.core.commands.NotHandledException | org.eclipse.core.commands.common.NotDefinedException e) {
        throw new IllegalStateException("Cannot open the active view menu", e);
      } finally {
        display.removeFilter(SWT.Show, listener);
        if (menu.get() != null && !menu.get().isDisposed()) {
          menu.get().setVisible(false);
        }
      }
    });
    if (menu.get() == null) {
      throw new IllegalStateException("The active view did not show a menu");
    }
    return menu.get();
  }
}
