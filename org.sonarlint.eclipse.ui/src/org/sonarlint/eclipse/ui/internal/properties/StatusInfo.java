/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * A settable IStatus.
 * Can be an error, warning, info or OKk. For error, info and warning states,
 * a message describes the problem.
 *
 */
class StatusInfo implements IStatus {

  private String fStatusMessage;
  private int fSeverity;

  /**
   * Creates a status set to OK (no message)
   */
  public StatusInfo() {
    this(OK, null);
  }

  /**
   * Creates a status .
   * @param severity The status severity: ERROR, WARNING, INFO and OK.
   * @param message The message of the status. Applies only for ERROR,
   * WARNING and INFO.
   */
  public StatusInfo(int severity, String message) {
    fStatusMessage = message;
    fSeverity = severity;
  }

  /**
   * Returns if the status' severity is OK.
   *
   * @return <code>true</code> if the status' severity is OK
   */
  @Override
  public boolean isOK() {
    return fSeverity == IStatus.OK;
  }

  /**
   * Returns if the status' severity is WARNING.
   *
   * @return <code>true</code> if the status' severity is WARNING
   */
  public boolean isWarning() {
    return fSeverity == IStatus.WARNING;
  }

  /**
   *  Returns if the status' severity is INFO.
   *
   * @return <code>true</code> if the status' severity is INFO
   */
  public boolean isInfo() {
    return fSeverity == IStatus.INFO;
  }

  /**
   *  Returns if the status' severity is ERROR.
   *
   * @return <code>true</code> if the status' severity is ERROR
   */
  public boolean isError() {
    return fSeverity == IStatus.ERROR;
  }

  /**
   * Returns the message.
   *
   * @return the message
   * @see IStatus#getMessage()
   */
  @Override
  public String getMessage() {
    return fStatusMessage;
  }

  /**
   * Sets the status to ERROR.
   * @param errorMessage the error message (can be empty, but not null)
   */
  public void setError(String errorMessage) {
    Assert.isNotNull(errorMessage);
    fStatusMessage = errorMessage;
    fSeverity = IStatus.ERROR;
  }

  /**
   * Sets the status to WARNING.
   * @param warningMessage the warning message (can be empty, but not null)
   */
  public void setWarning(String warningMessage) {
    Assert.isNotNull(warningMessage);
    fStatusMessage = warningMessage;
    fSeverity = IStatus.WARNING;
  }

  /**
   * Sets the status to INFO.
   * @param infoMessage the info message (can be empty, but not null)
   */
  public void setInfo(String infoMessage) {
    Assert.isNotNull(infoMessage);
    fStatusMessage = infoMessage;
    fSeverity = IStatus.INFO;
  }

  /**
   * Sets the status to OK.
   */
  public void setOK() {
    fStatusMessage = null;
    fSeverity = IStatus.OK;
  }

  /*
   * @see IStatus#matches(int)
   */
  @Override
  public boolean matches(int severityMask) {
    return (fSeverity & severityMask) != 0;
  }

  /**
   * Returns always <code>false</code>.
   * @see IStatus#isMultiStatus()
   */
  @Override
  public boolean isMultiStatus() {
    return false;
  }

  /*
   * @see IStatus#getSeverity()
   */
  @Override
  public int getSeverity() {
    return fSeverity;
  }

  /*
   * @see IStatus#getPlugin()
   */
  @Override
  public String getPlugin() {
    return SonarLintUiPlugin.PLUGIN_ID;
  }

  /**
   * Returns always <code>null</code>.
   * @see IStatus#getException()
   */
  @Override
  public Throwable getException() {
    return null;
  }

  /**
   * Returns always the error severity.
   * @see IStatus#getCode()
   */
  @Override
  public int getCode() {
    return fSeverity;
  }

  /**
   * Returns always <code>null</code>.
   * @see IStatus#getChildren()
   */
  @Override
  public IStatus[] getChildren() {
    return new IStatus[0];
  }

}
