/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2014 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.preferences;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;

/**
 * Preference page for the workspace.
 */
public class SonarPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public SonarPreferencePage() {
    super(Messages.SonarPreferencePage_title, GRID);
  }

  public void init(IWorkbench workbench) {
    setDescription(Messages.SonarPreferencePage_description);
    setPreferenceStore(SonarUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected void createFieldEditors() {

    addField(new ComboFieldEditor(SonarUiPlugin.PREF_MARKER_SEVERITY,
      Messages.SonarPreferencePage_label_marker_severity,
      new String[][] {
        {"Info", String.valueOf(IMarker.SEVERITY_INFO)},
        {"Warning", String.valueOf(IMarker.SEVERITY_WARNING)},
        {"Error", String.valueOf(IMarker.SEVERITY_ERROR)}},
      getFieldEditorParent()));
    addField(new ComboFieldEditor(SonarUiPlugin.PREF_NEW_ISSUE_MARKER_SEVERITY,
      Messages.SonarPreferencePage_label_new_issues_marker_severity,
      new String[][] {
        {"Info", String.valueOf(IMarker.SEVERITY_INFO)},
        {"Warning", String.valueOf(IMarker.SEVERITY_WARNING)},
        {"Error", String.valueOf(IMarker.SEVERITY_ERROR)}},
      getFieldEditorParent()));
    addField(new StringFieldEditor(SonarUiPlugin.PREF_JVM_ARGS,
      Messages.SonarPreferencePage_label_jvm_args, getFieldEditorParent()));
    addField(new BooleanFieldEditor(SonarUiPlugin.PREF_FORCE_FULL_PREVIEW,
      Messages.SonarPreferencePage_label_force_full_preview, BooleanFieldEditor.SEPARATE_LABEL, getFieldEditorParent()));
  }

}
