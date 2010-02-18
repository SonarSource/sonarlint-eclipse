package org.sonar.ide.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * Class used to initialize default preference values.
 *
 * @author Jérémie Lagarde
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = SonarPlugin.getDefault().getPreferenceStore();
    store.setDefault(PreferenceConstants.P_SONAR_SERVER_URL, PreferenceConstants.P_SONAR_SERVER_URL_DEFAULT);
  }

}
