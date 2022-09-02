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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.DialogPage;
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
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.ConnectionStorageUpdateJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.BindingsView;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.AuthMethod;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.job.SubscribeToNotificationsJob;
import org.sonarlint.eclipse.ui.internal.util.wizard.SonarLintWizardDialog;
import org.sonarsource.sonarlint.core.serverapi.exception.UnsupportedServerException;

public class ServerConnectionWizard extends Wizard implements INewWizard, IPageChangingListener {

  private final ServerConnectionModel model;
  private final ConnectionTypeWizardPage connectionTypeWizardPage;
  private final UrlWizardPage urlPage;
  private final AuthMethodWizardPage authMethodPage;
  private final UsernamePasswordWizardPage credentialsPage;
  private final TokenWizardPage tokenPage;
  private final OrganizationWizardPage orgPage;
  private final ConnectionIdWizardPage connectionIdPage;
  private final NotificationsWizardPage notifPage;
  private final ConfirmWizardPage confirmPage;
  private final IConnectedEngineFacade editedServer;
  private boolean redirectedAfterNotificationCheck;
  private boolean skipBindingWizard;

  private IConnectedEngineFacade resultServer;

  private ServerConnectionWizard(String title, ServerConnectionModel model, IConnectedEngineFacade editedServer) {
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
    connectionIdPage = new ConnectionIdWizardPage(model);
    notifPage = new NotificationsWizardPage(model);
    confirmPage = new ConfirmWizardPage(model);
  }

  /**
   * Should remain public for File -> New -> SonarQube ConnectedEngineFacade
   */
  public ServerConnectionWizard() {
    this(new ServerConnectionModel());
  }

  public ServerConnectionWizard(ServerConnectionModel model) {
    this("Connect to SonarQube or SonarCloud", model, null);
  }

  private ServerConnectionWizard(IConnectedEngineFacade sonarServer) {
    this(sonarServer.isSonarCloud() ? "Edit SonarCloud connection" : "Edit SonarQube connection", new ServerConnectionModel(sonarServer), sonarServer);
  }

  public static WizardDialog createDialog(Shell parent) {
    return new SonarLintWizardDialog(parent, new ServerConnectionWizard());
  }

  public static WizardDialog createDialog(Shell parent, List<ISonarLintProject> selectedProjects) {
    var model = new ServerConnectionModel();
    model.setSelectedProjects(selectedProjects);
    return new SonarLintWizardDialog(parent, new ServerConnectionWizard(model));
  }

  public static WizardDialog createDialog(Shell parent, String connectionId) {
    var model = new ServerConnectionModel();
    model.setConnectionId(connectionId);
    return new SonarLintWizardDialog(parent, new ServerConnectionWizard(model));
  }

  public static WizardDialog createDialog(Shell parent, IConnectedEngineFacade sonarServer) {
    return new SonarLintWizardDialog(parent, new ServerConnectionWizard(sonarServer));
  }

  public static WizardDialog createDialog(Shell parent, ServerConnectionWizard wizard) {
    return new SonarLintWizardDialog(parent, wizard);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // Nothing to do
  }

  public void setSkipBindingWizard(boolean skipBindingWizard) {
    this.skipBindingWizard = skipBindingWizard;
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
      addPage(connectionIdPage);
    }
    addPage(urlPage);
    addPage(authMethodPage);
    addPage(credentialsPage);
    addPage(tokenPage);
    addPage(orgPage);
    addPage(notifPage);
    addPage(confirmPage);
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
    if (page == connectionIdPage) {
      return notifPageIfSupportedOrConfirm();
    }
    if (page == notifPage) {
      return confirmPage;
    }
    return null;
  }

  private IWizardPage afterOrgPage() {
    return model.isEdit() ? notifPageIfSupportedOrConfirm() : connectionIdPage;
  }

  private IWizardPage notifPageIfSupportedOrConfirm() {
    return model.getNotificationsSupported() ? notifPage : confirmPage;
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
    var currentPage = getContainer().getCurrentPage();
    return currentPage == confirmPage;
  }

  @Override
  public boolean performFinish() {
    try {
      if (model.isEdit() && !testConnection(model.getOrganization())) {
        return false;
      }

      if (model.isEdit()) {
        editedServer.updateConfig(model.getServerUrl(), model.getOrganization(), model.getUsername(), model.getPassword(), model.getNotificationsDisabled());
        resultServer = editedServer;
      } else {
        finalizeConnectionCreation();
      }

      updateConnectionStorage();
      return true;
    } catch (Exception e) {
      var currentPage = (DialogPage) getContainer().getCurrentPage();
      currentPage.setErrorMessage("Cannot create connection: " + e.getMessage());
      SonarLintLogger.get().error("Error when finishing server connection wizard", e);
      return false;
    }
  }

  private void finalizeConnectionCreation() {
    resultServer = SonarLintCorePlugin.getServersManager().create(model.getConnectionId(), model.getServerUrl(), model.getOrganization(), model.getUsername(),
      model.getPassword(),
      model.getNotificationsDisabled());
    SonarLintCorePlugin.getServersManager().addServer(resultServer, model.getUsername(), model.getPassword());
    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BindingsView.ID);
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open SonarLint bindings view", e);
    }
  }

  private void updateConnectionStorage() {
    var job = new ConnectionStorageUpdateJob(resultServer);

    var boundProjects = resultServer.getBoundProjects();
    if (model.getNotificationsSupported() && !model.getNotificationsDisabled() && !boundProjects.isEmpty()) {
      var subscribeToNotificationsJob = new SubscribeToNotificationsJob(boundProjects);
      JobUtils.scheduleAfterSuccess(job, subscribeToNotificationsJob::schedule);
      subscribeToNotificationsJob.schedule();
    } else {
      boundProjects.forEach(SonarLintUiPlugin::unsubscribeToNotifications);
    }

    JobUtils.scheduleAfterSuccess(job, () -> JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(resultServer, TriggerType.BINDING_CHANGE));
    job.schedule();
    var selectedProjects = model.getSelectedProjects();
    if (!skipBindingWizard) {
      if (selectedProjects != null && !selectedProjects.isEmpty()) {
        ProjectBindingWizard
          .createDialogSkipServerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), selectedProjects, (ConnectedEngineFacade) resultServer)
          .open();
      } else if (boundProjects.isEmpty()) {
        ProjectBindingWizard
          .createDialogSkipServerSelection(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Collections.emptyList(), (ConnectedEngineFacade) resultServer)
          .open();
      }
    }
  }

  public IConnectedEngineFacade getResultServer() {
    return resultServer;
  }

  @Override
  public void handlePageChanging(PageChangingEvent event) {
    var currentPage = (WizardPage) event.getCurrentPage();
    boolean advance = getNextPage(currentPage) == event.getTargetPage();
    if (advance && !redirectedAfterNotificationCheck && (currentPage == credentialsPage || currentPage == tokenPage)) {
      if (!testConnection(null)) {
        event.doit = false;
        return;
      }
      // We need to wait for credentials before testing if notifications are supported
      populateNotificationsSupported();
      // Next page depends if notifications are supported
      var newNextPage = getNextPage(currentPage);
      if (newNextPage != event.getTargetPage()) {
        // Avoid infinite recursion
        redirectedAfterNotificationCheck = true;
        getContainer().showPage(newNextPage);
        redirectedAfterNotificationCheck = false;
        event.doit = false;
        return;
      }
    }
    if (advance && event.getTargetPage() == orgPage) {
      event.doit = tryLoadOrganizations(currentPage);
      return;
    }
    if (advance && currentPage == orgPage && model.hasOrganizations() && !testConnection(model.getOrganization())) {
      event.doit = false;
    }
  }

  private void populateNotificationsSupported() {
    if (model.getConnectionType() == ConnectionType.SONARCLOUD) {
      model.setNotificationsSupported(true);
      return;
    }
    try {
      getContainer().run(true, false, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          monitor.beginTask("Check if notifications are supported", IProgressMonitor.UNKNOWN);
          try {
            model
              .setNotificationsSupported(
                ConnectedEngineFacade.checkNotificationsSupported(model.getServerUrl(), model.getOrganization(), model.getUsername(), model.getPassword()));
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Unable to test notifications", e.getCause());
    } catch (InterruptedException e) {
      // Nothing to do, the task was simply canceled
    }
  }

  private boolean tryLoadOrganizations(WizardPage currentPage) {
    currentPage.setMessage(null);
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          try {
            var userOrgs = ConnectedEngineFacade.listUserOrganizations(model.getServerUrl(), model.getUsername(), model.getPassword(), monitor);
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
    var currentPage = getContainer().getCurrentPage();
    IStatus status;
    try {
      var testJob = new ServerConnectionTestJob(model.getServerUrl(), organization, model.getUsername(), model.getPassword());
      getContainer().run(true, true, testJob);
      status = testJob.getStatus();
    } catch (InterruptedException canceled) {
      return false;
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().error(message(e), e);
      status = new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error + " " + message(e), e);
    }

    var message = status.getMessage();
    if (status.getSeverity() == IStatus.CANCEL) {
      ((WizardPage) currentPage).setMessage(null, IMessageProvider.NONE);
      return false;
    }
    if (status.getSeverity() != IStatus.OK) {
      ((WizardPage) currentPage).setMessage(message, IMessageProvider.ERROR);
      return false;
    }

    return true;
  }

  private static String message(Exception e) {
    var message = e.getMessage();
    // Message is null for InvocationTargetException, look at the cause
    if (message != null) {
      return message;
    }
    if (e.getCause() != null && e.getCause().getMessage() != null) {
      return e.getCause().getMessage();
    }
    return "";
  }

}
