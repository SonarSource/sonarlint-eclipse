/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

/**
 * Open Sonar server URL in an embedded browser
 */
public abstract class AbstractSonarWebView extends ViewPart {

  private Browser browser;

  @Override
  public void createPartControl(Composite parent) {
    try {
      browser = new Browser(parent, SWT.FILL);
    } catch (SWTError e) {
      // Browser is probably not available but it will be partially initialized in parent
      for (Control c : parent.getChildren()) {
        c.dispose();
      }
      new Label(parent, SWT.WRAP).setText("Unable to create SWT Browser:\n " + e.getMessage());
    }
  }

  @Override
  public final void setFocus() {
    if (browser != null) {
      browser.setFocus();
    }
  }

  protected Browser getBrowser() {
    return browser;
  }

  protected void open(String url) {
    if (browser != null) {
      browser.setUrl(url);
    }
  }

  protected void showMessage(String message) {
    if (browser != null) {
      browser.setText("<p style=\"font: 13px arial,helvetica,clean,sans-serif;\">" + message + "</p>");
    }
  }

  protected void showHtml(String html) {
    if (browser != null) {
      browser.setText(html);
    }
  }

  protected IMarker findSelectedSonarIssue(ISelection selection) {
    try {
      if (selection instanceof IStructuredSelection) {
        List<IMarker> selectedSonarMarkers = new ArrayList<>();

        @SuppressWarnings("rawtypes")
        List elems = ((IStructuredSelection) selection).toList();
        for (Object elem : elems) {
          processElement(selectedSonarMarkers, elem);
        }

        if (!selectedSonarMarkers.isEmpty()) {
          return selectedSonarMarkers.get(0);
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static void processElement(List<IMarker> selectedSonarMarkers, Object elem) throws CoreException {
    if (elem instanceof IMarker) {
      IMarker marker = (IMarker) elem;
      if (isSonarLintMarker(marker)) {
        selectedSonarMarkers.add(marker);
      }
    } else if (elem instanceof IAdaptable) {
      IMarker marker = (IMarker) ((IAdaptable) elem).getAdapter(IMarker.class);
      if (marker != null && isSonarLintMarker(marker)) {
        selectedSonarMarkers.add(marker);
      }
    }
  }

  private static boolean isSonarLintMarker(IMarker marker) throws CoreException {
    return SonarLintCorePlugin.MARKER_ID.equals(marker.getType()) || SonarLintCorePlugin.MARKER_CHANGESET_ID.equals(marker.getType());
  }

}
