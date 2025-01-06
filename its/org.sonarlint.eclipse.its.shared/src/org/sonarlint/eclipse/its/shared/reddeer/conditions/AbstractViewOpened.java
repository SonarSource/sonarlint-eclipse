/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.conditions;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;

public class AbstractViewOpened<T extends WorkbenchView> implements WaitCondition {
  protected final Class<T> clazz;

  protected AbstractViewOpened(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean test() {
    try {
      clazz.getDeclaredConstructor().newInstance().isOpen();
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  @Override
  public T getResult() {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception ignored) {
      return null;
    }
  }

  @Override
  public String description() {
    return clazz.getName() + " view is opened";
  }

  @Override
  public String errorMessageWhile() {
    return clazz.getName() + " view is still opened";
  }

  @Override
  public String errorMessageUntil() {
    return clazz.getName() + " view is not yet opened";
  }
}
