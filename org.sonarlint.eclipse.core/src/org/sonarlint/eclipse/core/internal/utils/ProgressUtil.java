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
package org.sonarlint.eclipse.core.internal.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Progress Monitor utility.
 */
public class ProgressUtil {
  /**
   * ProgressUtil constructor comment.
   */
  private ProgressUtil() {
    super();
  }

  /**
   * Return a valid progress monitor.
   *
   * @param monitor org.eclipse.core.runtime.IProgressMonitor
   * @return org.eclipse.core.runtime.IProgressMonitor
   */
  public static IProgressMonitor getMonitorFor(IProgressMonitor monitor) {
    if (monitor == null)
      return new NullProgressMonitor();
    return monitor;
  }

  /**
   * Return a sub-progress monitor with the given amount on the
   * current progress monitor.
   *
   * @param monitor org.eclipse.core.runtime.IProgressMonitor
   * @param ticks int
   * @return org.eclipse.core.runtime.IProgressMonitor
   */
  public static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks) {
    if (monitor == null)
      return new NullProgressMonitor();
    if (monitor instanceof NullProgressMonitor)
      return monitor;
    return new SubProgressMonitor(monitor, ticks);
  }

  /**
   * Return a sub-progress monitor with the given amount on the
   * current progress monitor.
   *
   * @param monitor org.eclipse.core.runtime.IProgressMonitor
   * @param ticks a number of ticks
   * @param style a style
   * @return org.eclipse.core.runtime.IProgressMonitor
   */
  public static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks, int style) {
    if (monitor == null)
      return new NullProgressMonitor();
    if (monitor instanceof NullProgressMonitor)
      return monitor;
    return new SubProgressMonitor(monitor, ticks, style);
  }
}
