package org.sonarlint.eclipse.its.shared.reddeer.conditions;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;

/**
 *  When analyzing after a connection was established, make sure that everything was downloaded correctly and
 *  specifically await the CFamily analyzer to be available (for tests with Eclipse CDT).
 */
public class CFamilyLoaded extends AbstractWaitCondition {
  private static final String PATTERN = "CFamily Code Quality and Security";

  private final ConsoleView consoleView;

  public CFamilyLoaded(ConsoleView consoleView) {
    this.consoleView = consoleView;
  }

  @Override
  public boolean test() {
    var consoleText = consoleView.getConsoleText();
    return consoleText.lastIndexOf(PATTERN) != -1;
  }
}
