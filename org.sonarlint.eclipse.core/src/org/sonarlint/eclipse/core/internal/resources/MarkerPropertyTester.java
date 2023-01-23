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
package org.sonarlint.eclipse.core.internal.resources;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class MarkerPropertyTester extends PropertyTester {

  /**
   * Test if a marker corresponds to a standalone project.
   */
  @Override
  public boolean test(Object receiver, String property, Object[] args, @Nullable Object expectedValue) {
    var marker = Adapters.adapt(receiver, IMarker.class);
    if (marker == null) {
      return false;
    }

    var sonarLintFile = Adapters.adapt(marker.getResource(), ISonarLintFile.class);
    if (sonarLintFile == null) {
      return false;
    }
    return !SonarLintCorePlugin.loadConfig(sonarLintFile.getProject()).isBound();
  }
}
