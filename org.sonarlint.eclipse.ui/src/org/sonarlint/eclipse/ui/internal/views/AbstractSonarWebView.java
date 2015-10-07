/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.ui.internal.views;

import com.google.common.collect.Lists;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;

/**
 * Open Sonar server URL in an embedded browser
 */
public abstract class AbstractSonarWebView extends ViewPart {

  private Browser browser;

  @Override
  public void createPartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
  }

  @Override
  public final void setFocus() {
    browser.setFocus();
  }

  protected Browser getBrowser() {
    return browser;
  }

  protected void open(SonarLintProject sonarProject, String url) {
    browser.setUrl(url);
  }

  protected void showMessage(String message) {
    browser.setText("<p style=\"font: 13px arial,helvetica,clean,sans-serif;\">" + message + "</p>");
  }

  protected IMarker findSelectedSonarIssue(IWorkbenchPart part, ISelection selection) {
    try {
      if (selection instanceof IStructuredSelection) {
        List<IMarker> selectedSonarMarkers = Lists.newArrayList();

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

  private void processElement(List<IMarker> selectedSonarMarkers, Object elem) throws CoreException {
    if (elem instanceof IMarker) {
      IMarker marker = (IMarker) elem;
      if (SonarLintCorePlugin.MARKER_ID.equals(marker.getType())) {
        selectedSonarMarkers.add(marker);
      }
    } else if (elem instanceof IAdaptable) {
      IMarker marker = (IMarker) ((IAdaptable) elem).getAdapter(IMarker.class);
      if (marker != null && (SonarLintCorePlugin.MARKER_ID.equals(marker.getType()))) {
        selectedSonarMarkers.add(marker);
      }
    }
  }

}
