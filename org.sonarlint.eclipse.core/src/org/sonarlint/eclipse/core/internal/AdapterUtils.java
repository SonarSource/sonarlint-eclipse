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
package org.sonarlint.eclipse.core.internal;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

public class AdapterUtils {

  private AdapterUtils() {
    // Utility class
  }

  /**
   * Returns an object that is an instance of the given class associated with the given object. Returns <code>null</code> if no such object
   * can be found or if given object is <code>null</code>.
   */
  public static <T> T adapt(Object adaptable, Class<T> adapter) {
    if (adaptable == null) {
      return null;
    }
    if (adapter.isInstance(adaptable)) {
      return (T) adaptable;
    }
    Object result = null;
    if (adaptable instanceof IAdaptable) {
      result = ((IAdaptable) adaptable).getAdapter(adapter);
    }
    if (result == null) {
      // From IAdapterManager :
      // this method should be used judiciously, in order to avoid unnecessary plug-in activations
      result = Platform.getAdapterManager().loadAdapter(adaptable, adapter.getName());
    }
    return (T) result;
  }
}
