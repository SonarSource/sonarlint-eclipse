/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.ConnectionBindProjectsAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.ConnectionDeleteAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.ConnectionEditAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.ConnectionUpdateAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.NewConnectionWizardAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.ProjectChangeBindingAction;
import org.sonarlint.eclipse.ui.internal.binding.actions.ProjectUnbindAction;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;

public class BindingsViewActionProvider extends CommonActionProvider {
  public static final String NEW_MENU_ID = "org.sonarlint.eclipse.ui.server.newMenuId";

  private ICommonActionExtensionSite actionSite;
  protected Action deleteServerAction;
  protected Action editAction;
  protected Action updateAction;
  protected Action updateBindingAction;
  protected Action bindProjectsAction;
  protected Action unbindProjectsAction;

  public BindingsViewActionProvider() {
    super();
  }

  @Override
  public void init(ICommonActionExtensionSite aSite) {
    super.init(aSite);
    this.actionSite = aSite;
    var site = aSite.getViewSite();
    if (site instanceof ICommonViewerWorkbenchSite) {
      var viewer = aSite.getStructuredViewer();
      if (viewer instanceof CommonViewer) {
        var commonViewer = (CommonViewer) viewer;
        var wsSite = (ICommonViewerWorkbenchSite) site;
        addListeners(commonViewer);
        makeServerActions(commonViewer, wsSite.getSelectionProvider());
      }
    }
  }

  private static void addListeners(final CommonViewer tableViewer) {
    tableViewer.addOpenListener(event -> {
      var sel = (IStructuredSelection) event.getSelection();
      var data = sel.getFirstElement();
      if (data instanceof IConnectedEngineFacade) {
        var server = (IConnectedEngineFacade) data;
        ConnectionEditAction.openEditWizard(tableViewer.getTree().getShell(), server);
      } else if (data instanceof ISonarLintProject) {
        final var dialog = ProjectBindingWizard.createDialog(tableViewer.getTree().getShell(), List.of((ISonarLintProject) data));
        dialog.open();
      }
    });
  }

  private void makeServerActions(CommonViewer tableViewer, ISelectionProvider provider) {
    var shell = tableViewer.getTree().getShell();
    deleteServerAction = new ConnectionDeleteAction(shell, provider);
    editAction = new ConnectionEditAction(shell, provider);
    updateAction = new ConnectionUpdateAction(provider);
    updateBindingAction = new ProjectChangeBindingAction(shell, provider);
    bindProjectsAction = new ConnectionBindProjectsAction(shell, provider);
    unbindProjectsAction = new ProjectUnbindAction(shell, provider);
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    actionBars.updateActionBars();
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteServerAction);
    actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), editAction);
    actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), updateAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    // This is a temp workaround to clean up the default group that are provided by CNF
    menu.removeAll();

    var site = actionSite.getViewSite();
    IStructuredSelection selection = null;
    if (site instanceof ICommonViewerWorkbenchSite) {
      var wsSite = (ICommonViewerWorkbenchSite) site;
      selection = (IStructuredSelection) wsSite.getSelectionProvider().getSelection();
    }

    var servers = new ArrayList<IConnectedEngineFacade>();
    var projects = new ArrayList<ISonarLintProject>();
    populateServersAndProjects(selection, servers, projects);

    if (!servers.isEmpty() && projects.isEmpty()) {
      menu.add(updateAction);
      if (servers.size() == 1) {
        menu.add(bindProjectsAction);
      }
      menu.add(new Separator());
      if (servers.size() == 1) {
        menu.add(editAction);
      }
      menu.add(deleteServerAction);
      menu.add(new Separator());
    }

    if (projects.isEmpty()) {
      var newServerAction = new NewConnectionWizardAction();
      menu.add(newServerAction);
    } else if (servers.isEmpty()) {
      menu.add(updateBindingAction);
      menu.add(unbindProjectsAction);
    }

  }

  private static void populateServersAndProjects(IStructuredSelection selection, List<IConnectedEngineFacade> servers, List<ISonarLintProject> projects) {
    if (selection != null && !selection.isEmpty()) {
      var iterator = selection.iterator();
      while (iterator.hasNext()) {
        var obj = iterator.next();
        if (obj instanceof IConnectedEngineFacade) {
          servers.add((IConnectedEngineFacade) obj);
        } else if (obj instanceof ISonarLintProject) {
          projects.add((ISonarLintProject) obj);
        }
      }
    }
  }

}
