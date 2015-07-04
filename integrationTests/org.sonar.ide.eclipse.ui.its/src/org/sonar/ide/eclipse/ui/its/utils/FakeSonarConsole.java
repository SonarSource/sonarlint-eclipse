/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.utils;

import org.sonar.ide.eclipse.core.internal.jobs.LogListener;

public class FakeSonarConsole implements LogListener {

  @Override
  public void info(String msg) {
    System.out.print(msg);
  }

  @Override
  public void error(String msg) {
    System.err.print(msg);
  }

  @Override
  public void debug(String msg) {
    System.out.print(msg);
  }

}
