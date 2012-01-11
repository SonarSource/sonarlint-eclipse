/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.ui.console;

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
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

public class SonarConsolePageParticipant implements IConsolePageParticipant {

  private DebugAction debugAction;
  private ShowConsoleAction showOnOutputAction;

  public void init(IPageBookViewPage page, IConsole console) {
    debugAction = new DebugAction();
    showOnOutputAction = new ShowConsoleAction();

    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
  }

  private void configureToolBar(IToolBarManager manager) {
    manager.prependToGroup(IConsoleConstants.OUTPUT_GROUP, debugAction);
    manager.appendToGroup(IConsoleConstants.OUTPUT_GROUP, showOnOutputAction);
  }

  public void dispose() {
    debugAction.dispose();
    debugAction = null;

    showOnOutputAction.dispose();
    showOnOutputAction = null;
  }

  public void activated() {
  }

  public void deactivated() {
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

  static class ShowConsoleAction extends Action implements IMenuCreator {
    private Menu menu;

    public ShowConsoleAction() {
      setToolTipText(Messages.SonarShowConsoleAction_tooltip);
      setImageDescriptor(SonarImages.SHOW_CONSOLE);
      setMenuCreator(this);
    }

    public void dispose() {
      if (menu != null) {
        menu.dispose();
      }
    }

    public Menu getMenu(Control parent) {
      if (menu != null) {
        menu.dispose();
      }
      menu = new Menu(parent);
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_never_text, "never")); //$NON-NLS-1$
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_onOutput_text, "onOutput")); //$NON-NLS-1$
      addActionToMenu(menu, new MyAction(Messages.SonarShowConsoleAction_onError_text, "onError")); //$NON-NLS-1$
      return menu;
    }

    private void addActionToMenu(Menu parent, Action action) {
      ActionContributionItem item = new ActionContributionItem(action);
      item.fill(parent, -1);
    }

    public Menu getMenu(Menu parent) {
      return null;
    }

    private IPreferenceStore getPreferenceStore() {
      return SonarUiPlugin.getDefault().getPreferenceStore();
    }

    private String getCurrentValue() {
      return getPreferenceStore().getString(SonarConsole.P_SHOW_CONSOLE);
    }

    class MyAction extends Action {
      private final String value;

      public MyAction(String name, String value) {
        super(name, IAction.AS_RADIO_BUTTON);
        this.value = value;
        setChecked(value.equals(getCurrentValue()));
      }

      @Override
      public void run() {
        getPreferenceStore().setValue(SonarConsole.P_SHOW_CONSOLE, value);
      }
    }
  }

  static class DebugAction extends Action {
    private IPropertyChangeListener listener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (SonarConsole.P_DEBUG_OUTPUT.equals(event.getProperty())) {
          setChecked(isDebugEnabled());
        }
      }
    };

    public DebugAction() {
      setToolTipText(Messages.SonarDebugOutputAction_tooltip);
      setImageDescriptor(SonarImages.DEBUG);

      getPreferenceStore().addPropertyChangeListener(listener);
      setChecked(isDebugEnabled());
    }

    /**
     * Must be called to dispose this action.
     */
    public void dispose() {
      getPreferenceStore().removePropertyChangeListener(listener);
    }

    @Override
    public void run() {
      getPreferenceStore().setValue(SonarConsole.P_DEBUG_OUTPUT, isChecked());
    }

    private IPreferenceStore getPreferenceStore() {
      return SonarUiPlugin.getDefault().getPreferenceStore();
    }

    boolean isDebugEnabled() {
      return getPreferenceStore().getBoolean(SonarConsole.P_DEBUG_OUTPUT);
    }

  }

}
