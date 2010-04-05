package org.sonar.ide.eclipse.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.ui.ConsoleManager;
import org.sonar.ide.ui.ISonarConsole;

/**
 * @author Jérémie Lagarde
 */
public class SonarConsoleFactory extends ConsoleManager implements IConsoleFactory {

  public void openConsole() {
    showConsole();
  }

  protected ISonarConsole create() {
    return  SonarPlugin.getDefault().getConsole();
  }
  
  public static void showConsole() {
    IConsole console = SonarPlugin.getDefault().getConsole();
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = manager.getConsoles();
      boolean exists = false;
      for (int i = 0; i < existing.length; ++i) {
        if(console == existing[i])
          exists = true;
      }
      if(! exists)
        manager.addConsoles(new IConsole[] {console});
      manager.showConsoleView(console);
    }
  }
  
  public static void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    SonarConsole console = SonarPlugin.getDefault().getConsole();
    if (console != null) {
      manager.removeConsoles(new IConsole[] {console});
    }
  }
  
}