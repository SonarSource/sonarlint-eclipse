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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.wizard.SonarLintWizardDialog;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.FuzzySearchProjectsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.SonarProjectDto;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isEmpty;

public class ProjectBindingWizard extends Wizard implements INewWizard, IPageChangedListener {

  private static final String STORE_LAST_SELECTED_CONNECTION_ID = "ProjectBindingWizard.last_selected_server";

  private final ProjectBindingModel model;
  private final ConnectionSelectionWizardPage connectionSelectionWizardPage;
  private final SonarProjectSelectionWizardPage sonarProjectSelectionWizardPage;
  private final ProjectsSelectionWizardPage projectsSelectionWizardPage;

  private ProjectBindingWizard(String title, ProjectBindingModel model) {
    super();
    this.model = model;
    setNeedsProgressMonitor(true);
    setWindowTitle(title);
    setHelpAvailable(false);
    projectsSelectionWizardPage = new ProjectsSelectionWizardPage(model);
    setDialogSettings(SonarLintUiPlugin.getDefault().getDialogSettings());
    connectionSelectionWizardPage = new ConnectionSelectionWizardPage(model);
    sonarProjectSelectionWizardPage = new SonarProjectSelectionWizardPage(model);
  }

  private ProjectBindingWizard(Collection<ISonarLintProject> selectedProjects, @Nullable ConnectionFacade selectedConnection) {
    this("Bind to a SonarQube or SonarCloud project", new ProjectBindingModel());
    this.model.setProjects(selectedProjects.stream()
      .sorted(comparing(ISonarLintProject::getName))
      .collect(toCollection(ArrayList::new)));
    if (selectedConnection != null) {
      this.model.setConnection(selectedConnection);
      this.model.setSkipServer(true);
    } else if (SonarLintCorePlugin.getConnectionManager().getConnections().size() == 1) {
      // Only one server configured, pre-select it
      this.model.setConnection(SonarLintCorePlugin.getConnectionManager().getConnections().get(0));
    } else {
      var lastSelectedConnection = this.getDialogSettings().get(STORE_LAST_SELECTED_CONNECTION_ID);
      if (lastSelectedConnection != null) {
        SonarLintCorePlugin.getConnectionManager().findById(lastSelectedConnection)
          .ifPresent(this.model::setConnection);
      }
    }
    var projectKeys = selectedProjects.stream()
      .map(SonarLintCorePlugin::loadConfig)
      .map(SonarLintProjectConfiguration::getProjectBinding)
      .filter(Optional<EclipseProjectBinding>::isPresent)
      .map(Optional<EclipseProjectBinding>::get)
      .map(EclipseProjectBinding::getProjectKey)
      .collect(Collectors.toSet());
    if (projectKeys.size() == 1) {
      // If all projects are bound to the same Sonar project, then use it as default
      model.setSonarProjectKey(projectKeys.iterator().next());
    }
  }

  public static WizardDialog createDialogSkipConnectionSelection(Shell activeShell, Collection<ISonarLintProject> selectedProjects, ConnectionFacade selectedConnection) {
    return new SonarLintWizardDialog(activeShell, new ProjectBindingWizard(selectedProjects, selectedConnection));
  }

  public static WizardDialog createDialog(Shell activeShell, Collection<ISonarLintProject> selectedProjects) {
    if (SonarLintCorePlugin.getConnectionManager().getConnections().isEmpty()) {
      return ServerConnectionWizard.createDialog(activeShell);
    }
    return new SonarLintWizardDialog(activeShell, new ProjectBindingWizard(selectedProjects, null));
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // Nothing to do
  }

  @Override
  public IWizardPage getStartingPage() {
    if (model.getEclipseProjects().isEmpty()) {
      return projectsSelectionWizardPage;
    }
    if (!model.isSkipConnectionSelection()) {
      return connectionSelectionWizardPage;
    }
    return sonarProjectSelectionWizardPage;
  }

  @Override
  public void addPages() {
    addPage(projectsSelectionWizardPage);
    addPage(connectionSelectionWizardPage);
    addPage(sonarProjectSelectionWizardPage);
  }

  @Nullable
  @Override
  public IWizardPage getNextPage(IWizardPage page) {
    if (page == projectsSelectionWizardPage) {
      return model.isSkipConnectionSelection() ? sonarProjectSelectionWizardPage : connectionSelectionWizardPage;
    }
    if (page == connectionSelectionWizardPage) {
      return sonarProjectSelectionWizardPage;
    }
    return null;
  }

  @Nullable
  @Override
  public IWizardPage getPreviousPage(IWizardPage page) {
    // This method is only used for the first page of a wizard,
    // because every following page remember the previous one on its own
    if (page == connectionSelectionWizardPage) {
      return projectsSelectionWizardPage;
    }
    return null;
  }

  @Override
  public boolean canFinish() {
    var currentPage = getContainer().getCurrentPage();
    return currentPage == sonarProjectSelectionWizardPage && super.canFinish();
  }

  @Override
  public boolean performFinish() {
    var connection = model.getConnection();
    if (connection == null) {
      return false;
    } else {
      var connectionId = connection.getId();
      getDialogSettings().put(STORE_LAST_SELECTED_CONNECTION_ID, connectionId);
      ProjectBindingProcess.bindProjects(connectionId, model.getEclipseProjects(), model.getSonarProjectKey());

      // Every time a binding is created via the wizard then the binding was done manually, even if there were possible
      // suggestions - the user just turned them down or decided otherwise! For "automatic" binding (either from normal
      // SonarQube / SonarCloud property files found or the shared Connected Mode configurations) we always try to
      // create the connection and binding without invoking the UI at all!
      if (SonarLintTelemetry.isEnabled()) {
        SonarLintTelemetry.addedManualBindings();
      }

      return true;
    }
  }

  @Override
  public void pageChanged(PageChangedEvent event) {
    if (event.getSelectedPage() == sonarProjectSelectionWizardPage) {
      Display.getDefault().asyncExec(() -> {
        if (isEmpty(model.getSonarProjectKey())) {
          tryAutoBind();
        }
      });
    }
  }

  private void tryAutoBind() {
    SonarProjectDto bestCandidate = null;
    for (var project : model.getEclipseProjects()) {
      var response = SonarLintBackendService.get().getBackend().getConnectionService()
        .fuzzySearchProjects(new FuzzySearchProjectsParams(model.getConnection().getId(), project.getName()))
        .join();
      if (response.getTopResults().isEmpty()) {
        continue;
      }
      if (bestCandidate == null) {
        bestCandidate = response.getTopResults().get(0);
      } else if (!response.getTopResults().get(0).getKey().equals(bestCandidate.getKey())) {
        // Multiple best candidates, give up
        return;
      }
    }
    if (bestCandidate != null) {
      model.setSonarProjectKey(bestCandidate.getKey());
    }

  }
}
