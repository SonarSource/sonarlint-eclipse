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
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Violation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class load violations in background.
 *
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 *
 * @author Jérémie Lagarde
 */
public class RefreshViolationsJob extends AbstractRemoteSonarJob implements IResourceVisitor {

  private final List<IResource> resources;
  private IProgressMonitor monitor;
  private IStatus status;

  public RefreshViolationsJob(final List<IResource> resources) {
    super("Refresh violations");
    setPriority(Job.LONG);
    this.resources = resources;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    this.monitor = monitor;
    try {
      monitor.beginTask("Retrieve sonar data", resources.size());

      for (final IResource resource : resources) {
        if (!monitor.isCanceled() && resource.isAccessible()) {
          monitor.subTask("updating " + resource.getName());
          resource.accept(this);
        }
        monitor.worked(1);
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

  public boolean visit(final IResource resource) throws CoreException {
    if (resource instanceof IFile) {
      IFile file = (IFile) resource;
      retrieveMarkers(file, monitor);
      // do not visit members of this resource
      return false;
    }
    return true;
  }

  private void retrieveMarkers(final IFile resource, final IProgressMonitor monitor) {
    if ((resource == null) || !resource.exists() || monitor.isCanceled()) {
      return;
    }
    try {
      monitor.beginTask("Retrieve sonar informations for " + resource.getName(), 1);
      final Collection<Violation> violations = retrieveDatas(EclipseSonar.getInstance(resource.getProject()), resource);
      MarkerUtils.deleteViolationsMarkers(resource);
      for (final Violation violation : violations) {
        MarkerUtils.createMarkerForWSViolation(resource, violation, false);
      }
    } catch (final Exception ex) {
      LoggerFactory.getLogger(getClass()).error(ex.getMessage(), ex);
    } finally {
      monitor.done();
    }
  }

  protected Collection<Violation> retrieveDatas(EclipseSonar sonar, IResource resource) {
    SourceCode sourceCode = sonar.search(resource);
    if (sourceCode == null) {
      return Collections.emptyList();
    }
    return sourceCode.getViolations();
  }

  public static void setupViolationsUpdater() {
    new UIJob("Prepare violations updater") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        final IWorkbenchPage page = SonarUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.addPartListener(new ViolationsUpdater());
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  private static class ViolationsUpdater implements IPartListener2 {
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
              new RefreshViolationsJob(Collections.singletonList(resource)).schedule();
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
