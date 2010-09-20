/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.jobs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.progress.UIJob;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.shared.violations.ViolationUtils;
import org.sonar.wsclient.services.Violation;

/**
 * This class load violations in background.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 * 
 * @author Jérémie Lagarde
 */
public class RefreshViolationsJob extends AbstractRefreshModelJob<Violation> {

  public RefreshViolationsJob(final List<IResource> resources) {
    super(resources, SonarPlugin.MARKER_VIOLATION_ID);
  }

  @Override
  protected Collection<Violation> retrieveDatas(EclipseSonar sonar, IResource resource) {
    return sonar.search(resource).getViolations();
  }

  @Override
  protected Integer getLine(final Violation violation) {
    return violation.getLine();
  }

  @Override
  protected String getMessage(final Violation violation) {
    return ViolationUtils.getDescription(violation);
  }

  @Override
  protected Integer getPriority(final Violation violation) {
    if (ViolationUtils.PRIORITY_BLOCKER.equalsIgnoreCase(violation.getPriority())) {
      return new Integer(IMarker.PRIORITY_HIGH);
    }
    if (ViolationUtils.PRIORITY_CRITICAL.equalsIgnoreCase(violation.getPriority())) {
      return new Integer(IMarker.PRIORITY_HIGH);
    }
    if (ViolationUtils.PRIORITY_MAJOR.equalsIgnoreCase(violation.getPriority())) {
      return new Integer(IMarker.PRIORITY_NORMAL);
    }
    return new Integer(IMarker.PRIORITY_LOW);
  }

  @Override
  protected Integer getSeverity(final Violation violation) {
    return new Integer(IMarker.SEVERITY_WARNING);
  }

  @Override
  protected Map<String, Object> getExtraInfos(final Violation violation) {
    final Map<String, Object> extraInfos = new HashMap<String, Object>();
    extraInfos.put("rulekey", violation.getRuleKey());
    extraInfos.put("rulename", violation.getRuleName());
    extraInfos.put("rulepriority", violation.getPriority());
    return extraInfos;
  }

  public static void setupViolationsUpdater() {
    new UIJob("Prepare violations updater") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        final IWorkbenchPage page = SonarPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
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
          new RefreshViolationsJob(Collections.singletonList(resource)).schedule();
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
