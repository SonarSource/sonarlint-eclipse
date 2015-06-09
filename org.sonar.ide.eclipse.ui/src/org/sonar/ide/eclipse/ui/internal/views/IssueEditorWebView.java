/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.views;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.jobs.SynchronizeIssuesJob;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;

import java.util.Collections;

/**
 * Display details of an issue in a web browser
 */
public class IssueEditorWebView extends AbstractLinkedSonarWebView<IMarker> {

  public static final String ID = SonarUiPlugin.PLUGIN_ID + ".views.IssueEditorWebView";

  private IResource resource;

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    getBrowser().addProgressListener(new ProgressListener() {

      @Override
      public void completed(ProgressEvent event) {
        getBrowser().execute("$j(document).on('sonar.issue.updated', function(event, issueId) {eclipseIssueCallback(issueId);})");
      }

      @Override
      public void changed(ProgressEvent event) {
        // Nothing to do
      }
    });
    new CallbackFunction(getBrowser(), "eclipseIssueCallback");
  }

  private class CallbackFunction extends BrowserFunction {

    public CallbackFunction(Browser browser, String name) {
      super(browser, name);
    }

    @Override
    public Object function(Object[] arguments) {
      new SynchronizeIssuesJob(Collections.singletonList(resource), true).schedule();
      return null;
    }

  }

  @Override
  protected IMarker findSelectedElement(IWorkbenchPart part, ISelection selection) {
    return findSelectedSonarIssue(part, selection);
  }

  @Override
  protected void open(IMarker marker) {
    String issueId;
    try {
      if (SonarCorePlugin.NEW_ISSUE_MARKER_ID.equals(marker.getType())) {
        showMessage("It is not possible to edit a new issue.");
        return;
      }
      issueId = ObjectUtils.toString(marker.getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_ID_ATTR));
    } catch (CoreException e) {
      throw new IllegalStateException(e);
    }
    resource = marker.getResource();
    SonarProject sonarProject = SonarProject.getInstance(marker.getResource().getProject());
    String url = new SonarUrls().issueUrl(issueId, sonarProject.getUrl());
    super.open(sonarProject, url);
  }

}
