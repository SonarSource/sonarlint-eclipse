package org.sonar.ide.eclipse.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Jérémie Lagarde
 */
public class SonarConsolePageParticipant implements IConsolePageParticipant {

  private RemoveConsoleAction removeConsoleAction;

  public void init(IPageBookViewPage page, IConsole console) {
    this.removeConsoleAction = new RemoveConsoleAction();
    IActionBars bars = page.getSite().getActionBars();
    bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeConsoleAction);
  }

  public void dispose() {
    this.removeConsoleAction = null;
  }

  public void activated() {
  }

  public void deactivated() {
  }

  public Object getAdapter(Class adapter) {
    return null;
  }
}