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
package org.sonar.ide.eclipse.ui.internal.views;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;
import org.sonar.ide.eclipse.ui.internal.jobs.SynchronizeIssuesJob;

import java.util.Collections;

/**
 * Display details of an issue in a web browser
 */
public class IssueEditorWebView extends AbstractSonarWebView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.IssueEditorWebView";

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
      SonarProject sonarProject = SonarProject.getInstance(resource.getProject());
      if (!sonarProject.isAnalysedLocally()) {
        new SynchronizeIssuesJob(Collections.singletonList(resource), true).schedule();
      }
      return null;
    }

  }

  public void open(String issueId, IResource resource, IMarker marker) {
    this.resource = resource;
    SonarProject sonarProject = SonarProject.getInstance(resource.getProject());
    String url = new SonarUrls().issueUrl(issueId, sonarProject.getUrl());
    super.open(sonarProject, url);
  }

}
