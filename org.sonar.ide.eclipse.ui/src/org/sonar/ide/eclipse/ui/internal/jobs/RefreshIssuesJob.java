/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.jobs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.progress.UIJob;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.markers.SonarMarker;
import org.sonar.ide.eclipse.core.internal.remote.EclipseSonar;
import org.sonar.ide.eclipse.core.internal.remote.SourceCode;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.wsclient.ConnectionException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class load issues in background.
 *
 */
public class RefreshIssuesJob extends AbstractRemoteSonarJob implements IResourceProxyVisitor {

  private final List<IResource> resources;
  private IProgressMonitor monitor;
  private IStatus status;

  public RefreshIssuesJob(final List<IResource> resources) {
    super("Refresh issues");
    setPriority(Job.LONG);
    this.resources = resources;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    try {
      int scale = 1000;
      monitor.beginTask("Retrieve sonar data", resources.size() * scale);

      for (final IResource resource : resources) {
        if (monitor.isCanceled()) {
          break;
        }
        if (resource.isAccessible()) {
          this.monitor = new SubProgressMonitor(monitor, 1 * scale, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
          this.monitor.beginTask("Update " + resource.getName(), 1);
          try {
            resource.accept(this, IResource.NONE);
          } finally {
            monitor.done();
          }
        }
        else {
          monitor.worked(1 * scale);
        }
      }

      if (!monitor.isCanceled()) {
        status = Status.OK_STATUS;
      } else {
        status = Status.CANCEL_STATUS;
      }
    } catch (final ConnectionException e) {
      status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, IStatus.ERROR, "Unable to contact Sonar server", e);
    } catch (final Exception e) {
      status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e);
    } finally {
      monitor.done();
    }
    return status;
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

  public boolean visit(final IResourceProxy proxy) throws CoreException {
    if (proxy.getType() == IResource.FILE) {
      IFile file = (IFile) proxy.requestResource();
      retrieveMarkers(file, getMonitor());
      // do not visit members of this resource
      return false;
    }
    return true;
  }

  private void retrieveMarkers(final IFile resource, final IProgressMonitor monitor) {
    if ((resource == null) || !resource.exists() || monitor.isCanceled()) {
      return;
    }
    SonarProject sonarProject = SonarProject.getInstance(resource.getProject());
    EclipseSonar eclipseSonar = EclipseSonar.getInstance(resource.getProject());
    if (!MarkerUtils.needRefresh(resource, sonarProject, eclipseSonar.getSonarServer())) {
      return;
    }
    try {
      final Collection<ISonarIssue> issues = retrieveIssues(eclipseSonar, resource, monitor);
      MarkerUtils.deleteIssuesMarkers(resource);
      for (final ISonarIssue issue : issues) {
        SonarMarker.create(resource, false, issue);
      }
      MarkerUtils.updatePersistentProperties(resource, sonarProject, eclipseSonar.getSonarServer());
    } catch (final Exception ex) {
      LoggerFactory.getLogger(getClass()).error(ex.getMessage(), ex);
    }
  }

  protected Collection<ISonarIssue> retrieveIssues(EclipseSonar sonar, IResource resource, IProgressMonitor monitor) {
    SourceCode sourceCode = sonar.search(resource);
    if (sourceCode == null) {
      return Collections.emptyList();
    }
    return sourceCode.getRemoteIssuesWithLineCorrection(monitor);
  }

  public static void setupIssuesUpdater() {
    new UIJob("Prepare issues updater") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        final IWorkbenchPage page = SonarUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.addPartListener(new IssuesUpdater());
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  private static class IssuesUpdater implements IPartListener2 {
    public void partOpened(IWorkbenchPartReference partRef) {
      IWorkbenchPart part = partRef.getPart(true);
      if (part instanceof IEditorPart) {
        IEditorInput input = ((IEditorPart) part).getEditorInput();
        if (input instanceof IFileEditorInput) {
          IResource resource = ((IFileEditorInput) input).getFile();
          ISonarResource sonarResource = ResourceUtils.adapt(resource);
          if (sonarResource != null) {
            SonarProject projectProperties = SonarProject.getInstance(resource);
            if (!projectProperties.isAnalysedLocally()) {
              new RefreshIssuesJob(Collections.singletonList(resource)).schedule();
            }
          }
        }
      }
    }

    public void partVisible(IWorkbenchPartReference partRef) {
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

    public void partHidden(IWorkbenchPartReference partRef) {
    }

    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    public void partClosed(IWorkbenchPartReference partRef) {
    }

    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    public void partActivated(IWorkbenchPartReference partRef) {
    }
  }

}
