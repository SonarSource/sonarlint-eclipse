/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2022 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.its.reddeer.conditions;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

/**
 * Workaround for https://github.com/eclipse/reddeer/issues/2162
 *
 */
public class ConsoleHasLabel extends AbstractWaitCondition {

  private final Matcher<String> matcher;
  private final ConsoleView consoleView;
  private String resultLabel;

  /**
   * Construct the condition with a given text.
   *
   * @param text Text
   */
  public ConsoleHasLabel(ConsoleView consoleView, String text) {
    this(consoleView, new IsEqual<>(text));
  }

  /**
   * Instantiates a new console has label.
   *
   * @param matcher the matcher
   */
  public ConsoleHasLabel(ConsoleView consoleView, Matcher<String> matcher) {
    this.consoleView = consoleView;
    this.matcher = matcher;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.reddeer.common.condition.WaitCondition#test()
   */
  @Override
  public boolean test() {
    String consoleLabel = getConsoleLabel();
    if (matcher.matches(consoleLabel)) {
      this.resultLabel = consoleLabel;
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.reddeer.common.condition.AbstractWaitCondition#description()
   */
  @Override
  public String description() {
    return "console label matches '" + matcher;
  }

  private String getConsoleLabel() {
    consoleView.open();
    return consoleView.getConsoleLabel();
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getResult() {
    return this.resultLabel;
  }

}
