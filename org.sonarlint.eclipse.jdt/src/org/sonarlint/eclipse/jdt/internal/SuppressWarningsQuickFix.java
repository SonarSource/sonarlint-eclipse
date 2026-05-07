/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class SuppressWarningsQuickFix implements IMarkerResolution2 {
  @Override
  public String getLabel() {
    return "Disable warnings on \"@SuppressWarnings(...)\" for SonarQube issues";
  }

  @Override
  public String getDescription() {
    return "SonarQube uses custom ids that can be set in \"@SuppressWarnings\" to be picked up by the language "
      + "analyzers to suppress potential rule violations. Eclipse JDT does not recognize these IDs and reports them "
      + "as unsupported. This quick fix will disable this compiler warning.";
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_QUICKFIX_CHANGE;
  }

  @Override
  public void run(IMarker marker) {
    var options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
    JavaCore.setOptions(options);
  }
}
