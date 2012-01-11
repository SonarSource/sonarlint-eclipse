/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

public final class LogEntry {

  public static final int DEBUG = 0;
  public static final int INFO = 1;
  public static final int WARNING = 2;
  public static final int ERROR = 3;

  private int level;

  private String message;

  private Throwable exception;

  public LogEntry(int level, String message) {
    this(level, message, null);
  }

  public LogEntry(int level, String message, Throwable exception) {
    this.level = level;
    this.message = message;
    this.exception = exception;
  }

  public int getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public Throwable getException() {
    return exception;
  }

}
