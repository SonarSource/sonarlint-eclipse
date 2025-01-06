/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.job;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.backend.FileSystemSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.AbstractSonarJob;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.dialog.ProjectHierarchySelectionDialog;
import org.sonarlint.eclipse.ui.internal.dialog.ShareProjectBindingDialog;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class ShareProjectBindingJob extends AbstractSonarJob {
  private final Shell shell;
  private final ISonarLintProject project;

  public ShareProjectBindingJob(Shell shell, ISonarLintProject project) {
    super("Share project binding for project: " + project.getName());
    this.shell = shell;
    this.project = project;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    // 1) Check if project is part of hierarchy and check if root-projects are available
    var hierarchies = getHierarchies(project);

    // Thread safe information about the job status
    var status = new AtomicReference<IStatus>();
    status.set(Status.OK_STATUS);
    Display.getDefault().syncExec(() -> {
      // 2) Ask user to save it, behaves differently based on the hierarchy information:
      // - no hierarchy -> we can only save the info to THIS project
      // - one hierarchy AND root project -> we can save to either one of them
      // - one hierarchy AND no root project -> we can only save the info to THIS project
      // - more than one hierarchy -> user has to choose the correct one then proceed as above
      var isRootProject = false;
      var hasRootProject = false;
      ISonarLintProject rootProject = null;
      if (!hierarchies.isEmpty()) {
        var hierarchy = hierarchies.get(0);
        if (hierarchies.size() > 1) {
          // ask user which hierarchy they want to save it to
          var chooseDialog = new ProjectHierarchySelectionDialog(shell, hierarchies);
          if (chooseDialog.open() != 0) {
            status.set(Status.CANCEL_STATUS);
            return;
          }
          hierarchy = (String) chooseDialog.getFirstResult();
        }

        var temporaryRootProject = getRootProjectFromHierarchy(hierarchy, project);
        if (temporaryRootProject == null) {
          return;
        } else if (project.equals(temporaryRootProject)) {
          isRootProject = true;
        } else {
          hasRootProject = true;
          rootProject = temporaryRootProject;
        }
      }

      // 3) Handle the saving (or canceling) based on the user input
      var dialog = new ShareProjectBindingDialog(shell, isRootProject, hasRootProject);
      var result = dialog.open();
      if (hasRootProject && result == 0) {
        status.set(saveConnectedModeConfig(rootProject, monitor));
      } else if ((hasRootProject && result == 1) || result == 0) {
        status.set(saveConnectedModeConfig(project, monitor));
      } else if ((hasRootProject && result == 2) || result == 1) {
        BrowserUtils.openExternalBrowser(SonarLintDocumentation.CONNECTED_MODE_SHARING, shell.getDisplay());
      } else {
        status.set(Status.CANCEL_STATUS);
        return;
      }
    });

    return status.get();
  }

  /** Get all hierarchies for a specific project by their provider identifier */
  private static List<String> getHierarchies(ISonarLintProject project) {
    var hierarchies = new ArrayList<String>();
    for (var projectHierarchyProvider : SonarLintExtensionTracker.getInstance().getProjectHierarchyProviders()) {
      if (projectHierarchyProvider.partOfHierarchy(project)) {
        hierarchies.add(projectHierarchyProvider.getHierarchyProviderIdentifier());
      }
    }
    return hierarchies;
  }

  /**
   *  We only want to find the root project for a specific hierarchy, no need to check other providers after finding
   *  the required one!
   *
   *  @param hierarchy the specific provider we want to check
   *  @param project to find a root project
   *  @return root project if there is one, null otherwise
   */
  private static ISonarLintProject getRootProjectFromHierarchy(String hierarchy, ISonarLintProject project) {
    ISonarLintProject rootProject = null;
    for (var projectHierarchyProvider : SonarLintExtensionTracker.getInstance().getProjectHierarchyProviders()) {
      if (projectHierarchyProvider.getHierarchyProviderIdentifier() == hierarchy) {
        var tmpRootProject = projectHierarchyProvider.getRootProject(project);
        if (tmpRootProject != null) {
          rootProject = tmpRootProject;
        }
        break;
      }
    }
    return rootProject;
  }

  /**
   *  Tries to save the Connected Mode configuration to the specific file in the given project
   *
   *  @param project used for saving the configuration
   *  @param monitor for progress
   *  @return status that should be transferred to the job to report back
   */
  private static IStatus saveConnectedModeConfig(ISonarLintProject project, IProgressMonitor monitor) {
    try {
      var fileContent = JobUtils.waitForFuture(monitor,
        SonarLintBackendService.get().getSharedConnectedModeConfigFileContents(project))
        .getJsonFileContent();

      var iProject = (IProject) project.getResource();
      var folder = iProject.getFolder(FileSystemSynchronizer.SONARLINT_FOLDER);
      if (!folder.exists()) {
        folder.create(true, true, monitor);
      }
      var file = folder.getFile(FileSystemSynchronizer.SONARLINT_CONFIG_FILE);
      if (file.exists()) {
        file.delete(true, monitor);
      }
      file.create(new ByteArrayInputStream(fileContent.getBytes(Charset.defaultCharset())), true, monitor);
    } catch (Exception err) {
      SonarLintLogger.get().error("Saving the shared Connected Mode configuration to project '"
        + project.getName() + "' threw an error", err);
      return Status.error("Saving the shared Connected Mode configuration failed");
    }

    return Status.OK_STATUS;
  }
}
