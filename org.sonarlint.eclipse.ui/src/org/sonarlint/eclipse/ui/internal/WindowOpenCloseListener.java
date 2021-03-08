/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintFlowAnnotator;

class WindowOpenCloseListener implements IWindowListener {

  private static final OpenEditorAnalysisTrigger OPEN_EDITOR_ANALYSIS_TRIGGER = new OpenEditorAnalysisTrigger();
  private static final DeleteTaintMarkersOnEditorClosed DELETE_TAINT_MARKERS_ON_EDITOR_CLOSED = new DeleteTaintMarkersOnEditorClosed();

  private static final IPageListener PAGE_OPEN_CLOSE_LISTENER = new IPageListener() {

    @Override
    public void pageOpened(IWorkbenchPage page) {
      addListenersToPage(page);
    }

    @Override
    public void pageClosed(IWorkbenchPage page) {
      removeListenersFromPage(page);
    }

    @Override
    public void pageActivated(IWorkbenchPage page) {
      // Nothing to do here
    }
  };

  @Override
  public void windowOpened(IWorkbenchWindow window) {
    // Handle future opened/closed pages
    window.addPageListener(PAGE_OPEN_CLOSE_LISTENER);
    // Now we can attach listeners to existing pages
    addListenerToAllPages(window);
  }

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {
    // Nothing to do when user switch to another window
  }

  @Override
  public void windowClosed(IWorkbenchWindow window) {
    window.removePageListener(PAGE_OPEN_CLOSE_LISTENER);
    removeListenerFromAllPages(window);
  }

  @Override
  public void windowActivated(IWorkbenchWindow window) {
    // Nothing to do when user come back
  }

  static void addListenerToAllPages(IWorkbenchWindow window) {
    for (IWorkbenchPage page : window.getPages()) {
      addListenersToPage(page);
    }
  }

  private static void addListenersToPage(IWorkbenchPage page) {
    page.addPartListener(OPEN_EDITOR_ANALYSIS_TRIGGER);
    page.addPartListener(DELETE_TAINT_MARKERS_ON_EDITOR_CLOSED);
    page.addPartListener(SonarLintFlowAnnotator.PART_LISTENER);
    page.addPostSelectionListener(SonarLintUiPlugin.getSonarlintMarkerSelectionService());
  }

  static void removeListenerFromAllPages(IWorkbenchWindow window) {
    for (IWorkbenchPage page : window.getPages()) {
      removeListenersFromPage(page);
    }
  }

  private static void removeListenersFromPage(IWorkbenchPage page) {
    page.removePartListener(OPEN_EDITOR_ANALYSIS_TRIGGER);
    page.removePartListener(DELETE_TAINT_MARKERS_ON_EDITOR_CLOSED);
    page.removePartListener(SonarLintFlowAnnotator.PART_LISTENER);
    page.removePostSelectionListener(SonarLintUiPlugin.getSonarlintMarkerSelectionService());
  }
}
