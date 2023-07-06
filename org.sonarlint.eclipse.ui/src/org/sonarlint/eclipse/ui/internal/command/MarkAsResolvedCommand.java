/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.AddIssueCommentParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ChangeIssueStatusParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.CheckStatusChangePermittedParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.CheckStatusChangePermittedResponse;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.IssueStatus;


/**
 *  Command invoked on issues matched from SonarQube / SonarCloud to resolve an issue based on server configuration
 *  and user authentication. Not available on issues only found locally!
 */
public class MarkAsResolvedCommand extends AbstractIssueCommand implements IElementUpdater {
  @Override
  public void updateElement(UIElement element, Map parameters) {
    var window = element.getServiceLocator().getService(IWorkbenchWindow.class);
    if (window == null) {
      return;
    }
    var selection = (IStructuredSelection) window.getSelectionService().getSelection();
    var binding = getBinding(getSelectedMarker(selection));
    if (binding.isPresent()) {
      element.setIcon(binding.get().getEngineFacade().isSonarCloud() ? SonarLintImages.SONARCLOUD_16 : SonarLintImages.SONARQUBE_16);
    }
  }

  /** Check for issue binding: Either SonarQube or SonarCloud */
  private static Optional<ResolvedBinding> getBinding(IMarker marker) {
    var project = Adapters.adapt(marker.getResource().getProject(), ISonarLintProject.class);
    return SonarLintCorePlugin.getServersManager().resolveBinding(project);
  }

  @Override
  protected void execute(IMarker selectedMarker, IWorkbenchWindow window) {
    var issueKey = selectedMarker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null);
    if (issueKey == null) {
      // TODO: Show some kind of error!
      return;
    }
    
    var checkJob = new Job("Check user permissions for setting the issue resolution") {
      private CheckStatusChangePermittedResponse result;
      private ResolvedBinding resolvedBinding;

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        var binding = getBinding(selectedMarker);
        if (binding.isPresent()) {
          resolvedBinding = binding.get();
          try {
            result = JobUtils.waitForFuture(monitor, SonarLintBackendService.get().getBackend().getIssueService()
              .checkStatusChangePermitted(new CheckStatusChangePermittedParams(resolvedBinding.getProjectBinding().connectionId(), issueKey)));
            return Status.OK_STATUS;
          } catch (InvocationTargetException e) {
            return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
          } catch (InterruptedException e) {
            return new Status(IStatus.CANCEL, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
          }

        } else {
          return Status.error("Project is not bound anymore");
        }
      }

    };

    JobUtils.scheduleAfterSuccess(checkJob, () -> afterCheckSuccessful(issueKey, checkJob.result, checkJob.resolvedBinding, window));

    checkJob.schedule();
  }

  private static void afterCheckSuccessful(String issueId, CheckStatusChangePermittedResponse result,
    ResolvedBinding resolvedBinding, IWorkbenchWindow window) {
    if (!result.isPermitted()) {
      window.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(window.getShell(), "Mark Issue as Resolved on " + (resolvedBinding.getEngineFacade().isSonarCloud() ? "SonarCloud" : "SonarQube"),
          result.getNotPermittedReason()));
      return;
    }
    
    // TODO: Maybe wrap this in a job as well, otherwise at least the IssueService method calls!
    window.getShell().getDisplay().asyncExec(() -> {
      var shell = new Shell(window.getShell().getDisplay());
      shell.setLayout(new GridLayout(2, true));
      
      // All possible issue transitions listed below each other
      final var issueStatusCheckBoxButtons = new ArrayList<IssueStatusCheckBox>();
      result.getAllowedStatuses().forEach(transition -> {
        var checkBox = new IssueStatusCheckBox(shell, transition);

        checkBox.getCheckBox().setText(transition.getTitle() + ": " + transition.getDescription());
        var gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gridData.horizontalSpan = 2;
        checkBox.getCheckBox().setLayoutData(gridData);
        
        checkBox.getCheckBox().addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            issueStatusCheckBoxButtons.stream()
              .filter(it -> it.getCheckBox() != checkBox.getCheckBox())
              .forEach(it -> it.getCheckBox().setSelection(false));
            
            // TODO: Enable "Resolve issue" button
          }
        });
        
        issueStatusCheckBoxButtons.add(checkBox);
      });
      
      // Optional comment for the issue
      final var commentSection = new Text(shell, SWT.MULTI);
      var gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      gridData.horizontalSpan = 2;
      commentSection.setLayoutData(gridData);
      
      // Button to just cancel resolving the issue
      final var cancelButton = new Button(shell, SWT.NONE);
      cancelButton.setText("Cancel");
      cancelButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          shell.close();
        }
      });
      
      // Button to finish resolving the issue
      final var resolveButton = new Button(shell, SWT.NONE);
      resolveButton.setText("Resolve issue");
      resolveButton.setEnabled(false);
      resolveButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          // get the selected checkbox
          var selectedButton = issueStatusCheckBoxButtons.stream()
            .filter(it -> it.getCheckBox().getSelection())
            .findFirst()
            .get();
          
          // update the status on SQ / SC
          var issueService = SonarLintBackendService.get().getBackend().getIssueService();
          issueService.changeStatus(new ChangeIssueStatusParams(resolvedBinding.getEngineFacade().getId(),
            issueId,
            selectedButton.getIssueStatus(),
            /* TODO */ false));
          
          // check if text box contains text
          var comment = commentSection.getText();
          if (!comment.isBlank()) {
            issueService.addComment(new AddIssueCommentParams(resolvedBinding.getEngineFacade().getId(),
              issueId,
              comment));
          }
          
          shell.close();
          // jump to the post-resolve steps like updating the IDE view
          afterResolving();
        }
      });
      
      shell.open();
    });
  }
  
  private static void afterResolving() {
    //
  }
  
  /** Utility class to wrap a SWT Button (CheckBox) with its corresponding IssueStatus */
  static class IssueStatusCheckBox {
    private final Button checkBox;
    private final IssueStatus issueStatus;
    
    public IssueStatusCheckBox(Composite parent, IssueStatus issueStatus) {
      this.issueStatus = issueStatus;
      this.checkBox = new Button(parent, SWT.CHECK);
    }
    
    public Button getCheckBox() {
      return checkBox;
    }
    
    public IssueStatus getIssueStatus() {
      return issueStatus;
    }
  }
}
