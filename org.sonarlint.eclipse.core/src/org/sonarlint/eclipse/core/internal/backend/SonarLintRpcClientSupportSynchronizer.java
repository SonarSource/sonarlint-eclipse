/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.backend;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.eclipse.jdt.annotation.Nullable;

/**
 *  This synchronizes the information about Sloop (un-)available with the UI. Based on that information the UI can
 *  approach things differently: UI elements might contain different information (e.g. messaging), some information
 *  might not be available at all (e.g. context menu options).
 */
public class SonarLintRpcClientSupportSynchronizer {
  public static final String PROPERTY_NAME = "sloopAvailable";

  @Nullable
  private static PropertyChangeSupport support = new PropertyChangeSupport(new SonarLintRpcClientSupportSynchronizer());

  private static boolean sloopAvailable = false;

  public static void addListener(PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }

  public static void removeListener(PropertyChangeListener listener) {
    support.removePropertyChangeListener(listener);
  }

  public static boolean getSloopAvailability() {
    return sloopAvailable;
  }

  public static void setSloopAvailability(boolean available) {
    support.firePropertyChange(PROPERTY_NAME, sloopAvailable, available);
    sloopAvailable = available;
  }

  private SonarLintRpcClientSupportSynchronizer() {
    // following the "observer" pattern
  }
}
