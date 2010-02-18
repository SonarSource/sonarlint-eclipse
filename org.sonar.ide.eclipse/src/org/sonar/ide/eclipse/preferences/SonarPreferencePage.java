package org.sonar.ide.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class SonarPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

  public SonarPreferencePage() {
    super(GRID);
    setPreferenceStore(SonarPlugin.getDefault().getPreferenceStore());
    setDescription("Sonar server connection");
  }

  @Override
  public void createFieldEditors() {
    addField(new StringFieldEditor(PreferenceConstants.P_SONAR_SERVER_URL,
        "&Default sonar url:", getFieldEditorParent()));
  }

  public void init(IWorkbench workbench) {
  }


}