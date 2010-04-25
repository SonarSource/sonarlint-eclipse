package org.sonar.ide.eclipse.wizards;

import org.eclipse.ui.INewWizard;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

/**
 * @author Jérémie Lagarde
 */
public class NewServerLocationWizard extends AbstractServerLocationWizard implements INewWizard {

  protected String getTitle() {
    return Messages.getString("new.sonar.server"); //
  }

  protected String getDefaultUrl() {
    return SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
  }

}
