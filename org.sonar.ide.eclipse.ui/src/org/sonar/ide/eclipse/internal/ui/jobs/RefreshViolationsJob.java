/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.ui.jobs;

import java.util.*;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.UIJob;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
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
    super(resources, SonarCorePlugin.MARKER_ID);
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
    if (ViolationUtils.PRIORITY_BLOCKER.equalsIgnoreCase(violation.getSeverity())) {
      return Integer.valueOf(IMarker.PRIORITY_HIGH);
    }
    if (ViolationUtils.PRIORITY_CRITICAL.equalsIgnoreCase(violation.getSeverity())) {
      return Integer.valueOf(IMarker.PRIORITY_HIGH);
    }
    if (ViolationUtils.PRIORITY_MAJOR.equalsIgnoreCase(violation.getSeverity())) {
      return Integer.valueOf(IMarker.PRIORITY_NORMAL);
    }
    return Integer.valueOf(IMarker.PRIORITY_LOW);
  }

  @Override
  protected Integer getSeverity(final Violation violation) {
    return Integer.valueOf(IMarker.SEVERITY_WARNING);
  }

  @Override
  protected Map<String, Object> getExtraInfos(final Violation violation) {
    final Map<String, Object> extraInfos = new HashMap<String, Object>();
    extraInfos.put("rulekey", violation.getRuleKey());
    extraInfos.put("rulename", violation.getRuleName());
    extraInfos.put("rulepriority", violation.getSeverity());
    return extraInfos;
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
          ISonarResource sonarResource = PlatformUtils.adapt(resource, ISonarResource.class);
          if (sonarResource != null) {
            new RefreshViolationsJob(Collections.singletonList(resource)).schedule();
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
