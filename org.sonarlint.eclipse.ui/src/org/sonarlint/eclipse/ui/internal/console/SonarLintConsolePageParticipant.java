/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class SonarLintConsolePageParticipant implements IConsolePageParticipant {

  private DebugAction debugAction;
  private ShowConsoleAction showOnOutputAction;
  private IAction consoleRemoveAction;

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    this.consoleRemoveAction = new RemoveAction();
    this.debugAction = new DebugAction();
    this.showOnOutputAction = new ShowConsoleAction();

    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
  }

  private void configureToolBar(IToolBarManager manager) {
    manager.prependToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
    manager.prependToGroup(IConsoleConstants.OUTPUT_GROUP, debugAction);
    manager.appendToGroup(IConsoleConstants.OUTPUT_GROUP, showOnOutputAction);
  }

  @Override
  public void dispose() {
    debugAction.dispose();
    debugAction = null;

    showOnOutputAction.dispose();
    showOnOutputAction = null;
  }

  @Override
  public void activated() {
    // Nothing to do
  }

  @Override
  public void deactivated() {
    // Nothing to do
  }

  @Override
  public Object getAdapter(Class adapter) {
    return null;
  }

  static class ShowConsoleAction extends Action implements IMenuCreator {
    private Menu menu;

    public ShowConsoleAction() {
      setToolTipText(Messages.SonarShowConsoleAction_tooltip);
      setImageDescriptor(SonarLintImages.SHOW_CONSOLE);
      setMenuCreator(this);
    }

    @Override
    public void dispose() {
      if (menu != null) {
        menu.dispose();
      }
    }

    @Override
    public Menu getMenu(Control parent) {
      if (menu != null) {
        menu.dispose();
      }
      menu = new Menu(parent);
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_never_text, SonarLintConsole.P_SHOW_CONSOLE_NEVER));
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_onOutput_text, SonarLintConsole.P_SHOW_CONSOLE_ON_OUTPUT));
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_onError_text, SonarLintConsole.P_SHOW_CONSOLE_ON_ERROR));
      return menu;
    }

    private static void addActionToMenu(Menu parent, Action action) {
      ActionContributionItem item = new ActionContributionItem(action);
      item.fill(parent, -1);
    }

    @Override
    public Menu getMenu(Menu parent) {
      return null;
    }

    private static IPreferenceStore getPreferenceStore() {
      return SonarLintUiPlugin.getDefault().getPreferenceStore();
    }

    private static String getCurrentValue() {
      return getPreferenceStore().getString(SonarLintConsole.P_SHOW_CONSOLE);
    }

    static class MyAction extends Action {
      private final String value;

      public MyAction(String name, String value) {
        super(name, IAction.AS_RADIO_BUTTON);
        this.value = value;
        setChecked(value.equals(getCurrentValue()));
      }

      @Override
      public void run() {
        getPreferenceStore().setValue(SonarLintConsole.P_SHOW_CONSOLE, value);
      }
    }
  }

  static class DebugAction extends Action {
    private final IPropertyChangeListener listener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (SonarLintConsole.P_DEBUG_OUTPUT.equals(event.getProperty())) {
          setChecked(SonarLintConsole.isDebugEnabled());
        }
      }
    };

    public DebugAction() {
      setToolTipText(Messages.SonarDebugOutputAction_tooltip);
      setImageDescriptor(SonarLintImages.DEBUG);

      getPreferenceStore().addPropertyChangeListener(listener);
      setChecked(SonarLintConsole.isDebugEnabled());
    }

    /**
     * Must be called to dispose this action.
     */
    public void dispose() {
      getPreferenceStore().removePropertyChangeListener(listener);
    }

    @Override
    public void run() {
      getPreferenceStore().setValue(SonarLintConsole.P_DEBUG_OUTPUT, isChecked());
    }

    private static IPreferenceStore getPreferenceStore() {
      return SonarLintUiPlugin.getDefault().getPreferenceStore();
    }

  }

}
