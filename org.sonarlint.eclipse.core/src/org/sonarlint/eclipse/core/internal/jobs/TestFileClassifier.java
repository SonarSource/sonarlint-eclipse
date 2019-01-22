/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
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
    String allTestPattern = PreferencesUtils.getTestFileRegexps();
    String[] testPatterns = allTestPattern.split(",");
    pathMatchersForTests = createMatchersForTests(testPatterns);
  }

  private static List<PathMatcher> createMatchersForTests(String[] testPatterns) {
    final List<PathMatcher> pathMatchers = new ArrayList<>();
    FileSystem fs = FileSystems.getDefault();
    for (String testPattern : testPatterns) {
      pathMatchers.add(fs.getPathMatcher("glob:" + testPattern));
    }
    return pathMatchers;
  }

  public boolean isTest(ISonarLintFile file) {
    for (IFileTypeProvider typeProvider : SonarLintCorePlugin.getExtensionTracker().getTypeProviders()) {
      switch (typeProvider.qualify(file)) {
        case UNKNOWN:
          break;
        case MAIN:
          SonarLintLogger.get().debug("File '" + file.getProjectRelativePath() + "' qualified as main by '" + typeProvider.getClass().getSimpleName() + "'");
          return false;
        case TEST:
          SonarLintLogger.get().debug("File '" + file.getProjectRelativePath() + "' qualified as test by '" + typeProvider.getClass().getSimpleName() + "'");
          return true;
      }
    }
    Path fileRelativePath = Paths.get(file.getProjectRelativePath());
    for (PathMatcher matcher : pathMatchersForTests) {
      if (matcher.matches(fileRelativePath)) {
        return true;
      }
    }
    return false;
  }
}
