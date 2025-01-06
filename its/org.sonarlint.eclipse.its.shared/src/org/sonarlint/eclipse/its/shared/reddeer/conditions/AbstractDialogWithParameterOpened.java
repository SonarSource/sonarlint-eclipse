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

import org.eclipse.reddeer.swt.impl.shell.DefaultShell;

public abstract class AbstractDialogWithParameterOpened<T extends DefaultShell, P>
  extends AbstractDialogOpened<T> {
  protected final Class<P> parameterType;
  protected final Object parameterValue;

  protected AbstractDialogWithParameterOpened(Class<T> clazz, Class<P> parameterType, Object parameterValue) {
    super(clazz);
    this.parameterType = parameterType;
    this.parameterValue = parameterValue;
  }

  @Override
  public boolean test() {
    try {
      clazz.getDeclaredConstructor(parameterType).newInstance(parameterValue).isEnabled();
      return true;
    } catch (Exception ignored) {
      System.out.println("Error: " + ignored);
      return false;
    }
  }

  @Override
  public T getResult() {
    try {
      return clazz.getDeclaredConstructor(parameterType).newInstance(parameterValue);
    } catch (Exception ignored) {
      return null;
    }
  }
}
