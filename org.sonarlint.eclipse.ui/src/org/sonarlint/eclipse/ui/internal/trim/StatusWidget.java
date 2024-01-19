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
package org.sonarlint.eclipse.ui.internal.trim;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.BindingsView;
import org.sonarlint.eclipse.ui.internal.hotspots.HotspotsView;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionWebView;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;
import org.sonarlint.eclipse.ui.internal.views.issues.SonarLintReportView;
import org.sonarlint.eclipse.ui.internal.views.issues.TaintVulnerabilitiesView;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

public class StatusWidget extends WorkbenchWindowControlContribution {

  @Nullable
  private Composite trimComposite = null;

  @Override
  public void dispose() {
    if (trimComposite != null && !trimComposite.isDisposed()) {
      trimComposite.dispose();
    }
    trimComposite = null;
  }

  @Override
  protected Control createControl(Composite parent) {
    trimComposite = new Composite(parent, SWT.NONE);
    trimComposite.setLayout(createLayout());

    var icon = new Label(trimComposite, SWT.CENTER);
    icon.setImage(SonarLintImages.STATUS_IMG);

    createContextMenu(icon);
    updateToolTip(icon);

    return trimComposite;
  }

  private RowLayout createLayout() {
    var type = getOrientation();
    var layout = new RowLayout(type);
    layout.marginTop = 2;
    layout.marginBottom = 2;
    layout.marginLeft = 2;
    layout.marginRight = 2;
    return layout;
  }

  private static void createContextMenu(Control c) {
    var menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(StatusWidget::fillMenu);
    var menu = menuMgr.createContextMenu(c);
    c.setMenu(menu);

    alsoOpenMenuWithLeftClick(c, menu);
  }

  private static void alsoOpenMenuWithLeftClick(Control c, Menu menu) {
    c.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent event) {
        super.mouseUp(event);
        var absolutePos = ((Control) event.widget).toDisplay(event.x, event.y);
        menu.setLocation(absolutePos);
        menu.setVisible(true);
      }
    });
  }

  private static void fillMenu(IMenuManager menuMgr) {
    var showViewSubMenu = new MenuManager("Show View", null);
    showViewSubMenu.add(new ShowViewAction("On-The-Fly", OnTheFlyIssuesView.ID, SonarLintImages.VIEW_ON_THE_FLY, "Display issues found by the on-the-fly analysis"));
    showViewSubMenu.add(new ShowViewAction("Report", SonarLintReportView.ID, SonarLintImages.VIEW_REPORT, "Display issues found by manually triggered analyses"));
    showViewSubMenu.add(new ShowViewAction("Rule Description", RuleDescriptionWebView.ID, SonarLintImages.VIEW_RULE, "Display rule description for the selected issue"));
    showViewSubMenu.add(new ShowViewAction("Bindings", BindingsView.ID, SonarLintImages.VIEW_BINDINGS, "Allow to configure connections and bindings for SonarLint connected mode"));
    showViewSubMenu.add(new ShowViewAction("Security Hotspots", HotspotsView.ID, SonarLintImages.VIEW_HOTSPOTS, "Show security hotspots opened from SonarQube"));
    showViewSubMenu.add(new ShowViewAction("Issue locations", IssueLocationsView.ID, SonarLintImages.VIEW_LOCATIONS, "Show secondary locations or flows for the selected issue"));
    showViewSubMenu.add(new ShowViewAction("Taint Vulnerabilities", TaintVulnerabilitiesView.ID, SonarLintImages.VIEW_VULNERABILITIES,
      "Show taint vulnerabilities found by SonarQube or SonarCloud"));
    menuMgr.add(showViewSubMenu);

    menuMgr.add(new OpenGloblaSettingsAction());
    menuMgr.add(new ShowConsoleAction());
  }

  private static void updateToolTip(Control icon) {
    var text = "SonarLint";
    if (!text.equals(icon.getToolTipText())) {
      icon.setToolTipText(text);
    }
  }

  static class ShowViewAction extends Action {
    private final String viewId;

    ShowViewAction(String label, String viewId, ImageDescriptor image, String description) {
      super(label);
      this.viewId = viewId;
      setImageDescriptor(image);
      setToolTipText(description);
    }

    @Override
    public void run() {
      var wbw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (wbw != null) {
        try {
          wbw.getActivePage().showView(viewId);
        } catch (Exception e) {
          SonarLintLogger.get().error("Unable to open view", e);
        }
      }
    }
  }

  static class OpenGloblaSettingsAction extends Action {

    OpenGloblaSettingsAction() {
      super("Preferences...");
      setDescription("Open SonarLint Global Preferences");
    }

    @Override
    public void run() {
      PlatformUtils.showPreferenceDialog(SonarLintPreferencePage.ID).open();
    }
  }

  static class ShowConsoleAction extends Action {

    ShowConsoleAction() {
      super("Show Console");
      setImageDescriptor(ConsolePlugin.getImageDescriptor(IConsoleConstants.IMG_VIEW_CONSOLE));
      setDescription("Open SonarLint Logs in the Console View");
    }

    @Override
    public void run() {
      SonarLintUiPlugin.getDefault().getSonarConsole().bringConsoleToFront();
    }
  }

}
