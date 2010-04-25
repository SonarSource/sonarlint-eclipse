package org.sonar.ide.eclipse.wizards;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.INewWizard;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class EditServerLocationWizard extends AbstractServerLocationWizard implements INewWizard {

  private String oldServerUrl;
  
  public EditServerLocationWizard(String serverUrl) {
    super();
    oldServerUrl = serverUrl;
  }

  protected String getTitle() {
    return Messages.getString("action.edit.server.desc"); //$NON-NLS-1$
  }

  protected String getDefaultUrl() {
    return oldServerUrl;
  }
  
  protected void doFinish(String serverUrl, String username, String password, IProgressMonitor monitor) throws Exception {
    if(StringUtils.isNotBlank(oldServerUrl) && SonarPlugin.getServerManager().findServer(oldServerUrl)!= null )
    SonarPlugin.getServerManager().removeServer(oldServerUrl);
    super.doFinish(serverUrl, username, password,monitor);
  }
}
