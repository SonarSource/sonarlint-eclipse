package org.sonar.ide.eclipse.console;

import org.eclipse.jface.action.Action;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class RemoveConsoleAction extends Action {

  RemoveConsoleAction() {
    setToolTipText(Messages.getString("console.view.remove.label")); //$NON-NLS-1$
    setImageDescriptor(SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONARCLOSE));
  }
  
  public void run() {
    SonarConsoleFactory.closeConsole();
  }
}