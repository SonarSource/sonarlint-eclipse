/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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

public abstract class AbstractDialogWithTwoParametersOpened<T extends DefaultShell, P1, P2>
  extends AbstractDialogWithParameterOpened<T, P1> {
  protected final Class<P2> parameterType2;
  protected final Object parameterValue2;

  protected AbstractDialogWithTwoParametersOpened(Class<T> clazz, Class<P1> parameterType1, Object parameterValue1,
    Class<P2> parameterType2, Object parameterValue2) {
    super(clazz, parameterType1, parameterValue1);
    this.parameterType2 = parameterType2;
    this.parameterValue2 = parameterValue2;
  }

  @Override
  public boolean test() {
    try {
      clazz.getDeclaredConstructor(parameterType, parameterType2).newInstance(parameterValue, parameterValue2)
        .isEnabled();
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  @Override
  public T getResult() {
    try {
      return clazz.getDeclaredConstructor(parameterType, parameterType2).newInstance(parameterValue, parameterValue2);
    } catch (Exception ignored) {
      return null;
    }
  }
}
