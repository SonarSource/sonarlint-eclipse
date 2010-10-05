/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.core.SonarLogger;

import java.net.MalformedURLException;
import java.net.URL;

public class SendFeedbackAction implements IWorkbenchWindowActionDelegate {

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    IWorkbenchBrowserSupport browserSupport = SonarPlugin.getDefault().getWorkbench().getBrowserSupport();
    try {
      URL url = new URL("http://jira.codehaus.org/browse/SONARIDE");
      if (browserSupport.isInternalWebBrowserAvailable()) {
        browserSupport.createBrowser(null).openURL(url);
      } else {
        browserSupport.getExternalBrowser().openURL(url);
      }
    } catch (PartInitException e) {
      SonarLogger.log(e);
    } catch (MalformedURLException e) {
      SonarLogger.log(e);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

}
