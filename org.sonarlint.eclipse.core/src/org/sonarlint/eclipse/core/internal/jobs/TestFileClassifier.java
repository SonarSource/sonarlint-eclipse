/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.jobs;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider.ISonarLintFileType;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class TestFileClassifier {
  private static TestFileClassifier instance;
  private List<PathMatcher> pathMatchersForTests;

  public TestFileClassifier() {
    reload();
  }

  public static TestFileClassifier get() {
    if (instance == null) {
      instance = new TestFileClassifier();
    }
    return instance;
  }

  /**
   * Reload patterns from global preferences.
   * Should be called when preferences are changed.
   */
  public void reload() {
    var allTestPattern = SonarLintGlobalConfiguration.getTestFileGlobPatterns();
    var testPatterns = allTestPattern.split(",");
    pathMatchersForTests = createMatchersForTests(testPatterns);
  }

  private static List<PathMatcher> createMatchersForTests(String[] testPatterns) {
    var fs = FileSystems.getDefault();
    return Stream.of(testPatterns)
      .map(pattern -> fs.getPathMatcher("glob:" + pattern))
      .collect(Collectors.toList());
  }

  public boolean isTest(ISonarLintFile file) {
    for (var typeProvider : SonarLintExtensionTracker.getInstance().getTypeProviders()) {
      if (typeProvider.qualify(file) == ISonarLintFileType.TEST) {
        SonarLintLogger.get().traceIdeMessage("File '" + file.getProjectRelativePath() + "' qualified as test by '" + typeProvider.getClass().getSimpleName() + "'");
        return true;
      }
    }
    var fileRelativePath = Paths.get(file.getProjectRelativePath());
    for (var matcher : pathMatchersForTests) {
      if (matcher.matches(fileRelativePath)) {
        SonarLintLogger.get().debug("File '" + file.getProjectRelativePath() + "' qualified as test by file pattern");
        return true;
      }
    }
    return false;
  }
}
