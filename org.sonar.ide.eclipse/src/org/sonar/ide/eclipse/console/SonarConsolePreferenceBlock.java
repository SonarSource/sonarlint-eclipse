package org.sonar.ide.eclipse.console;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

/**
 * @author Jérémie Lagarde
 */
public class SonarConsolePreferenceBlock {

  private ColorFieldEditor   requestColorEditor;
  private ColorFieldEditor   responseColorEditor;
  private ColorFieldEditor   errorColorEditor;
  private BooleanFieldEditor showOnMessage;
  private BooleanFieldEditor showOnError;
  private BooleanFieldEditor restrictOutput;

  public Control createContents(Composite parent) {
    IPreferenceStore preferenceStore = SonarPlugin.getDefault().getPreferenceStore();

    // Create group
    Composite container = new Composite(parent, SWT.NULL);
    Group group = new Group(parent, SWT.NONE);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    group.setLayoutData(data);
    group.setText(Messages.getString("pref.global.label.console")); //$NON-NLS-1$
    GridLayout gridLayout = new GridLayout(2, false);
    group.setLayout(gridLayout);

    // Create check boxes option.
    showOnError = new BooleanFieldEditor(PreferenceConstants.P_CONSOLE_SHOW_ON_ERROR, Messages.getString("pref.global.label.console.showOnError"), group); //$NON-NLS-1$
    showOnError.setPreferenceName(PreferenceConstants.P_CONSOLE_SHOW_ON_ERROR);
    showOnError.setPreferenceStore(preferenceStore);
    showOnError.load();

    showOnMessage = new BooleanFieldEditor(PreferenceConstants.P_CONSOLE_SHOW_ON_MESSAGE, Messages.getString("pref.global.label.console.showOnMessage"), group); //$NON-NLS-1$
    showOnMessage.setPreferenceName(PreferenceConstants.P_CONSOLE_SHOW_ON_MESSAGE);
    showOnMessage.setPreferenceStore(preferenceStore);
    showOnMessage.load();

    // Create color editors.
    requestColorEditor = createColorFieldEditor(PreferenceConstants.P_CONSOLE_REQUEST_COLOR, Messages.getString("pref.global.label.console.requestColor"), group); //$NON-NLS-1$
    requestColorEditor.setPreferenceName(PreferenceConstants.P_CONSOLE_REQUEST_COLOR);
    requestColorEditor.setPreferenceStore(preferenceStore);
    requestColorEditor.load();

    responseColorEditor = createColorFieldEditor(PreferenceConstants.P_CONSOLE_RESPONSE_COLOR, Messages.getString("pref.global.label.console.responseColor"), group); //$NON-NLS-1$
    responseColorEditor.setPreferenceName(PreferenceConstants.P_CONSOLE_RESPONSE_COLOR);
    responseColorEditor.setPreferenceStore(preferenceStore);
    responseColorEditor.load();

    errorColorEditor = createColorFieldEditor(PreferenceConstants.P_CONSOLE_ERROR_COLOR, Messages.getString("pref.global.label.console.errorColor"), group); //$NON-NLS-1$
    errorColorEditor.setPreferenceName(PreferenceConstants.P_CONSOLE_ERROR_COLOR);
    errorColorEditor.setPreferenceStore(preferenceStore);
    errorColorEditor.load();
    return container;
  }

  public void performApply(IPreferenceStore preferenceStore) {
    preferenceStore.setValue(PreferenceConstants.P_CONSOLE_SHOW_ON_ERROR, showOnError.getBooleanValue());
    preferenceStore.setValue(PreferenceConstants.P_CONSOLE_SHOW_ON_MESSAGE, showOnMessage.getBooleanValue());
    requestColorEditor.store();
    responseColorEditor.store();
    errorColorEditor.store();
  }

  private ColorFieldEditor createColorFieldEditor(String preferenceName, String label, Composite parent) {
    IPreferenceStore preferenceStore = SonarPlugin.getDefault().getPreferenceStore();
    ColorFieldEditor editor = new ColorFieldEditor(preferenceName, label, parent);
    editor.setPreferenceStore(preferenceStore);
    return editor;
  }
}
