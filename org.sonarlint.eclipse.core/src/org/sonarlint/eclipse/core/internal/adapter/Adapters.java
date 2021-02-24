/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.adapter;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Copied from Oxygen for backward compatibility with older versions of Eclipse
 * TODO use org.eclipse.core.runtime.Adapters when minimal version will be Oxygen
 */
public class Adapters {
  /**
   * If it is possible to adapt the given object to the given type, this
   * returns the adapter. Performs the following checks:
   * 
   * <ol>
   * <li>Returns <code>sourceObject</code> if it is an instance of the
   * adapter type.</li>
   * <li>If sourceObject implements IAdaptable, it is queried for adapters.</li>
   * <li>Finally, the adapter manager is consulted for adapters</li>
   * </ol>
   * 
   * Otherwise returns null.
   * 
   * @param sourceObject
   *            object to adapt, can be null
   * @param adapter
   *            type to adapt to
   * @param allowActivation
   *            if true, plug-ins may be activated if necessary to provide the requested adapter.
   *            if false, the method will return null if an adapter cannot be provided from activated plug-ins.
   * @return a representation of sourceObject that is assignable to the
   *         adapter type, or null if no such representation exists
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T adapt(@Nullable Object sourceObject, Class<T> adapter, boolean allowActivation) {
    if (sourceObject == null) {
      return null;
    }
    if (adapter.isInstance(sourceObject)) {
      return (T) sourceObject;
    }

    if (sourceObject instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) sourceObject;

      Object result = adaptable.getAdapter(adapter);
      if (result != null) {
        // Sanity-check
        if (!adapter.isInstance(result)) {
          throw new AssertionFailedException(adaptable.getClass().getName() + ".getAdapter(" + adapter.getName() + ".class) returned " //$NON-NLS-1$//$NON-NLS-2$
            + result.getClass().getName() + " that is not an instance the requested type"); //$NON-NLS-1$
        }
        return (T) result;
      }
    }

    // If the source object is a platform object then it's already tried calling AdapterManager.getAdapter,
    // so there's no need to try it again.
    if ((sourceObject instanceof PlatformObject) && !allowActivation) {
      return null;
    }

    String adapterId = adapter.getName();
    Object result = queryAdapterManager(sourceObject, adapterId, allowActivation);
    if (result != null) {
      // Sanity-check
      if (!adapter.isInstance(result)) {
        throw new AssertionFailedException("An adapter factory for " //$NON-NLS-1$
          + sourceObject.getClass().getName() + " returned " + result.getClass().getName() //$NON-NLS-1$
          + " that is not an instance of " + adapter.getName()); //$NON-NLS-1$
      }
      return (T) result;
    }

    return null;
  }

  /**
   * If it is possible to adapt the given object to the given type, this
   * returns the adapter.
   * <p>
   * Convenience method for calling <code>adapt(Object, Class, true)</code>.
   * <p>
   * See {@link #adapt(Object, Class, boolean)}.
   * 
   * @param sourceObject
   *            object to adapt, can be null
   * @param adapter
   *            type to adapt to
   * @return a representation of sourceObject that is assignable to the
   *         adapter type, or null if no such representation exists
   */
  @Nullable
  public static <T> T adapt(Object sourceObject, Class<T> adapter) {
    return adapt(sourceObject, adapter, true);
  }

  @Nullable
  private static Object queryAdapterManager(Object sourceObject, String adapterId, boolean allowActivation) {
    Object result;
    if (allowActivation) {
      result = Platform.getAdapterManager().loadAdapter(sourceObject, adapterId);
    } else {
      result = Platform.getAdapterManager().getAdapter(sourceObject, adapterId);
    }
    return result;
  }
}
