/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.bind.wizard.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.server.ServersView;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.server.wizard.ServerConnectionModel.AuthMethod;
import org.sonarlint.eclipse.ui.internal.server.wizard.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.util.wizard.WizardDialogWithoutHelp;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;
import org.sonarsource.sonarlint.core.client.api.exceptions.UnsupportedServerException;

public class ServerConnectionWizard extends Wizard implements INewWizard, IPageChangingListener {

  private final ServerConnectionModel model;
  private final ConnectionTypeWizardPage connectionTypeWizardPage;
  private final UrlWizardPage urlPage;
  private final AuthMethodWizardPage authMethodPage;
  private final UsernamePasswordWizardPage credentialsPage;
  private final TokenWizardPage tokenPage;
  private final OrganizationWizardPage orgPage;
  private final ServerIdWizardPage serverIdPage;
  private final EndWizardPage endPage;
  private final IServer editedServer;

  private ServerConnectionWizard(String title, ServerConnectionModel model, IServer editedServer) {
    super();
    this.model = model;
    this.editedServer = editedServer;
    setNeedsProgressMonitor(true);
    setWindowTitle(title);
    setHelpAvailable(false);
    connectionTypeWizardPage = new ConnectionTypeWizardPage(model);
    urlPage = new UrlWizardPage(model);
    authMethodPage = new AuthMethodWizardPage(model);
    credentialsPage = new UsernamePasswordWizardPage(model);
    tokenPage = new TokenWizardPage(model);
    orgPage = new OrganizationWizardPage(model);
    serverIdPage = new ServerIdWizardPage(model);
    endPage = new EndWizardPage(model);
  }

  /**
   * Should remain public for File -> New -> SonarQube Server
   */
  public ServerConnectionWizard() {
    this(new ServerConnectionModel());
  }

  private ServerConnectionWizard(ServerConnectionModel model) {
    this("Connect to SonarQube or SonarCloud", model, null);
  }

  private ServerConnectionWizard(IServer sonarServer) {
    this(sonarServer.isSonarCloud() ? "Edit SonarCloud connection" : "Edit SonarQube connection", new ServerConnectionModel(sonarServer), sonarServer);
  }

  public static WizardDialog createDialog(Shell parent) {
    return new WizardDialogWithoutHelp(parent, new ServerConnectionWizard());
  }

  public static WizardDialog createDialog(Shell parent, List<ISonarLintProject> selectedProjects) {
    ServerConnectionModel model = new ServerConnectionModel();
    model.setSelectedProjects(selectedProjects);
    return new WizardDialogWithoutHelp(parent, new ServerConnectionWizard(model));
  }

  public static WizardDialog createDialog(Shell parent, String serverId) {
    ServerConnectionModel model = new ServerConnectionModel();
    model.setServerId(serverId);
    return new WizardDialogWithoutHelp(parent, new ServerConnectionWizard(model));
  }

  public static WizardDialog createDialog(Shell parent, IServer sonarServer) {
    return new WizardDialogWithoutHelp(parent, new ServerConnectionWizard(sonarServer));
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // Nothing to do
  }

  @Override
  public IWizardPage getStartingPage() {
    if (!model.isEdit()) {
      return connectionTypeWizardPage;
    }
    return firstPageAfterConnectionType();
  }

  @Override
  public void addPages() {
    if (!model.isEdit()) {
      addPage(connectionTypeWizardPage);
      addPage(serverIdPage);
    }
    addPage(urlPage);
    addPage(authMethodPage);
    addPage(credentialsPage);
    addPage(tokenPage);
    addPage(orgPage);
    addPage(endPage);
  }

  @Override
  public IWizardPage getNextPage(IWizardPage page) {
    if (page == connectionTypeWizardPage) {
      return firstPageAfterConnectionType();
    }
    if (page == urlPage) {
      return authMethodPage;
    }
    if (page == authMethodPage) {
      return model.getAuthMethod() == AuthMethod.PASSWORD ? credentialsPage : tokenPage;
    }
    if (page == credentialsPage || page == tokenPage) {
      if (model.getConnectionType() == ConnectionType.SONARCLOUD) {
        return orgPage;
      } else {
        return afterOrgPage();
      }
    }
    if (page == orgPage) {
      return afterOrgPage();
    }
    if (page == serverIdPage) {
      return endPage;
    }
    return null;
  }

  private IWizardPage afterOrgPage() {
    return model.isEdit() ? endPage : serverIdPage;
  }

  @Override
  public IWizardPage getPreviousPage(IWizardPage page) {
    // This method is only used for the first page of a wizard,
    // because every following page remember the previous one on its own
    return null;
  }

  private IWizardPage firstPageAfterConnectionType() {
    // Skip URL and auth method page if SonarCloud
    return model.getConnectionType() == ConnectionType.SONARCLOUD ? tokenPage : urlPage;
  }

  @Override
  public boolean canFinish() {
    IWizardPage currentPage = getContainer().getCurrentPage();
    return currentPage == endPage;
  }

  @Override
  public boolean performFinish() {
    if (model.isEdit() && !testConnection(model.getOrganization())) {
      return false;
    }
    IServer server;

    if (model.isEdit()) {
      editedServer.updateConfig(model.getServerUrl(), model.getOrganization(), model.getUsername(), model.getPassword(), model.getNotificationsEnabled());
      server = editedServer;

    } else {
      server = SonarLintCorePlugin.getServersManager().create(model.getServerId(), model.getServerUrl(), model.getOrganization(), model.getUsername(), model.getPassword(),
        model.getNotificationsEnabled());
      SonarLintCorePlugin.getServersManager().addServer(server, model.getUsername(), model.getPassword());
      try {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ServersView.ID);
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Unable to open server view", e);
      }
    }
    Job job = new ServerUpdateJob(server);

    List<ISonarLintProject> boundProjects = server.getBoundProjects();
    if (model.getNotificationsSupported() && model.getNotificationsEnabled() && !boundProjects.isEmpty()) {
      Job subscribeToNotificationsJob = new Job("Subscribe to notifications") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          boundProjects.forEach(SonarLintUiPlugin::subscribeToNotifications);
          return Status.OK_STATUS;
        }
      };
      JobUtils.scheduleAfterSuccess(job, subscribeToNotificationsJob::schedule);
    } else {
      boundProjects.forEach(SonarLintUiPlugin::unsubscribeToNotifications);
    }

    JobUtils.scheduleAfterSuccess(job, () -> JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(server, TriggerType.BINDING_CHANGE));
    job.schedule();
    List<ISonarLintProject> selectedProjects = model.getSelectedProjects();
    if (selectedProjects != null && !selectedProjects.isEmpty()) {
      ProjectBindingWizard.createDialogSkipServerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), selectedProjects, (Server) server).open();
    } else if (boundProjects.isEmpty()) {
      ProjectBindingWizard.createDialogSkipServerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Collections.emptyList(), (Server) server).open();
    }
    return true;
  }

  @Override
  public void handlePageChanging(PageChangingEvent event) {
    WizardPage currentPage = (WizardPage) event.getCurrentPage();
    boolean advance = getNextPage(currentPage) == event.getTargetPage();
    if (advance && (currentPage == credentialsPage || currentPage == tokenPage) && !testConnection(null)) {
      event.doit = false;
      return;
    }
    if (advance && event.getTargetPage() == orgPage) {
      event.doit = tryLoadOrganizations(currentPage);
      return;
    }
    if (advance && currentPage == orgPage && model.hasOrganizations() && !testConnection(model.getOrganization())) {
      event.doit = false;
      return;
    }
    if (advance && event.getTargetPage() == endPage) {
      boolean notificationsSupported = Server.checkNotificationsSupported(model.getServerUrl(), model.getOrganization(), model.getUsername(), model.getPassword());
      endPage.setNotificationsSupported(notificationsSupported);
      model.setNotificationsSupported(notificationsSupported);
      if (notificationsSupported && !model.isEdit()) {
        model.setNotificationsEnabled(true);
      }
    }
  }

  private boolean tryLoadOrganizations(WizardPage currentPage) {
    currentPage.setMessage(null);
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          try {
            List<RemoteOrganization> userOrgs = Server.listUserOrganizations(model.getServerUrl(), model.getUsername(), model.getPassword(), monitor);
            model.setUserOrgs(userOrgs);
          } catch (UnsupportedServerException e) {
            model.setUserOrgs(null);
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Unable to download organizations", e.getCause());
      currentPage.setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      model.setUserOrgs(null);
      return false;
    } catch (InterruptedException e) {
      model.setUserOrgs(null);
      return false;
    }
    return true;
  }

  private boolean testConnection(@Nullable String organization) {
    IWizardPage currentPage = getContainer().getCurrentPage();
    IStatus status;
    try {
      ServerConnectionTestJob testJob = new ServerConnectionTestJob(model.getServerUrl(), organization, model.getUsername(), model.getPassword());
      getContainer().run(true, true, testJob);
      status = testJob.getStatus();
    } catch (OperationCanceledException e1) {
      return false;
    } catch (Exception e1) {
      status = new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error + " " +
        e1.getMessage(), e1);
    }

    String message = status.getMessage();
    if (status.getSeverity() != IStatus.OK) {
      ((WizardPage) currentPage).setMessage(message, IMessageProvider.ERROR);
      return false;
    }

    return true;
  }
}
