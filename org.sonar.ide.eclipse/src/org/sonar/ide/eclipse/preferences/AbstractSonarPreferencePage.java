package org.sonar.ide.eclipse.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sonar.ide.eclipse.SonarPlugin;

public abstract class AbstractSonarPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  protected AbstractSonarPreferencePage() {
  }

  protected AbstractSonarPreferencePage(String title) {
    super(title);
  }

  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    return SonarPlugin.getDefault().getPreferenceStore();
  }

  /**
   * {@inheritDoc}
   */
  public void init(IWorkbench workbench) {
  }

  @Override
  public boolean performOk() {
    performApply();
    return super.performOk();
  }

}
