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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sonarlint.eclipse.core.SonarLintLogger;

/**
 *  Generator for the Quick Fox on the "Unsupported @SuppressWarnings(...)" marker that is added when the value(s)
 *  provided is one indicating a SonarQube-related rule - these are unknown to Eclipse JDT.
 */
public class SuppressWarningsQuickFixGenerator implements IMarkerResolutionGenerator2 {
  static final String JAVA_PROBLEM_TYPE = "org.eclipse.jdt.core.problem"; //$NON-NLS-1$
  static final String MARKER_MESSAGE_GENERAL = "Unsupported @SuppressWarnings"; //$NON-NLS-1$
  static final String MARKER_MESSAGE_JAVA_ISSUE = "java:"; //$NON-NLS-1$
  static final String MARKER_MESSAGE_JAVA_SECURITY_ISSUE = "javasecurity:"; //$NON-NLS-1$
  static final String MARKER_MESSAGE_JAVA_BUGS_ISSUE = "javabugs:"; //$NON-NLS-1$
  static final String MARKER_MESSAGE_JAVA_ARCHITECTURE_ISSUE = "javaarchitecture:"; //$NON-NLS-1$

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    if (isSuppressWarningsJavaProblem(marker)) {
      return new IMarkerResolution[] {new SuppressWarningsQuickFix()};
    }
    return new IMarkerResolution[0];
  }

  @Override
  public boolean hasResolutions(IMarker marker) {
    return isSuppressWarningsJavaProblem(marker);
  }

  private boolean isSuppressWarningsJavaProblem(IMarker marker) {
    if (!JavaProjectConfiguratorExtension.isJdtPresent()) {
      return false;
    }

    try {
      var isJavaProblem = marker.getType().equals(JAVA_PROBLEM_TYPE);
      var message = (String) marker.getAttribute(IMarker.MESSAGE);
      return isJavaProblem && message != null && message.contains(MARKER_MESSAGE_GENERAL)
        && (message.toLowerCase().contains(MARKER_MESSAGE_JAVA_ISSUE)
          || message.toLowerCase().contains(MARKER_MESSAGE_JAVA_SECURITY_ISSUE)
          || message.toLowerCase().contains(MARKER_MESSAGE_JAVA_BUGS_ISSUE)
          || message.toLowerCase().contains(MARKER_MESSAGE_JAVA_ARCHITECTURE_ISSUE));
    } catch (CoreException err) {
      SonarLintLogger.get().error(err.getMessage(), err);
      return false;
    }
  }
}
