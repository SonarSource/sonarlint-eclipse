/*
 * Cxdopyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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
package org.sonar.ide.eclipse.internal.ui.texteditor.coverage;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to toggle the coverage bar's and color information in the editor. When
 * turned on, sonar ide plugin shows the coverage informations form sonar server
 * in the editor .
 * 
 * @author Jérémie Lagarde
 * @since 0.2.0
 */
public class CoverageToggleAction implements IEditorActionDelegate, IUpdate {

  /** The editor we are working on. */
  ITextEditor editor = null;
  IAction     action = null;

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    if (targetEditor instanceof ITextEditor) {
      editor = (ITextEditor) targetEditor;
      this.action = action;
      update();
    } else {
      editor = null;
    }
  }

  public void run(IAction action) {
    if (editor == null) {
      return;
    }
    toggleCoverageRuler();
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

  public void update() {
    if (action != null) {
      action.setChecked(isCoverageRulerVisible());
    }
  }

  /**
   * Toggles the coverage global preference and shows the sonar coverage ruler
   * accordingly.
   */
  private void toggleCoverageRuler() {
    IPreferenceStore store = EditorsUI.getPreferenceStore();
    store.setValue(CoverageColumn.SONAR_COVERAGE_RULER, !isCoverageRulerVisible());
  }

  /**
   * Returns whether the sonar coverage ruler column should be visible according
   * to the preference store settings.
   */
  private boolean isCoverageRulerVisible() {
    IPreferenceStore store = EditorsUI.getPreferenceStore();
    return store != null ? store.getBoolean(CoverageColumn.SONAR_COVERAGE_RULER) : false;
  }
}
