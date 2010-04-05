package org.sonar.ide.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
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
    store.setDefault(PreferenceConstants.P_CONSOLE_REQUEST_COLOR, StringConverter.asString(new RGB(0, 255, 50)));
    store.setDefault(PreferenceConstants.P_CONSOLE_RESPONSE_COLOR, StringConverter.asString(new RGB(100, 100, 255)));
    store.setDefault(PreferenceConstants.P_CONSOLE_ERROR_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
  }

}
