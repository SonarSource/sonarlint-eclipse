/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarsource.sonarlint.core.commons.progress.ClientProgressMonitor;

public class WrappedProgressMonitor implements ClientProgressMonitor {

  private final IProgressMonitor wrapped;
  private int worked = 0;

  public WrappedProgressMonitor(IProgressMonitor wrapped, String taskName) {
    this.wrapped = wrapped;
    wrapped.beginTask(taskName, 100);
  }

  @Override
  public boolean isCanceled() {
    return wrapped.isCanceled();
  }

  @Override
  public void setFraction(float fraction) {
    int total = (int) (fraction * 100);
    wrapped.worked(total - worked);
    this.worked = total;
  }

  @Override
  public void setMessage(String msg) {
    wrapped.subTask(msg);
  }

  @Override
  public void setIndeterminate(boolean arg0) {
    // Not available in Eclipse
  }

}
