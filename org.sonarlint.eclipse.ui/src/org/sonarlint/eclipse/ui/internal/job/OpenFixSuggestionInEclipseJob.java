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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.client.fix.FixSuggestionDto;

/**
 *  "Open fix suggestion": For a specific file there are "one-to-many" suggestions coming to be displayed to the user
 *                         one after another. The behavior before the actual logic is similar to the one implemented
 *                         for the "Open in IDE" feature.
 */
public class OpenFixSuggestionInEclipseJob extends AbstractOpenInEclipseJob {
  private final FixSuggestionDto fixSuggestion;

  public OpenFixSuggestionInEclipseJob(FixSuggestionDto fixSuggestion, ISonarLintProject project) {
    super("Open fix suggestion in IDE", project, true);

    this.fixSuggestion = fixSuggestion;
  }

  @Override
  IStatus actualRun() throws CoreException {
    // To be implemented later as this is currently only the skeleton!
    SonarLintLogger.get().info("Open fix suggestion: " + fixSuggestion.suggestionId());

    return Status.OK_STATUS;
  }

  @Override
  String getIdeFilePath() {
    return fixSuggestion.fileEdit().idePath().toString();
  }
}
