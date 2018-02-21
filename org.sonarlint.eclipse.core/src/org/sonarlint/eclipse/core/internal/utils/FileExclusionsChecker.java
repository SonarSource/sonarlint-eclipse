/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.FileExclusions;

public class FileExclusionsChecker {
  private FileExclusions projectExclusions;
  private FileExclusions globalExclusions;

  public FileExclusionsChecker(ISonarLintProject project) {
    SonarLintProjectConfiguration projectConfiguration = SonarLintProjectConfiguration.read(project.getScopeContext());
    List<ExclusionItem> projectExclusionItems = projectConfiguration.getFileExclusions();
    List<ExclusionItem> globalExclusionItems = PreferencesUtils.getFileExclusions(project);

    Set<String> projectFileExclusions = getExclusionsOfType(projectExclusionItems, Type.FILE);
    Set<String> projectDirectoryExclusions = getExclusionsOfType(projectExclusionItems, Type.DIRECTORY);
    Set<String> projectGlobExclusions = getExclusionsOfType(projectExclusionItems, Type.GLOB);
    Set<String> globalGlobExclusions = getExclusionsOfType(globalExclusionItems, Type.GLOB);

    projectExclusions = new FileExclusions(projectFileExclusions, projectDirectoryExclusions, projectGlobExclusions);
    globalExclusions = new FileExclusions(globalGlobExclusions);
  }

  public boolean shouldAnalyze(ISonarLintFile file) {
    return shouldAnalyze(file, false);
  }

  public boolean shouldAnalyze(ISonarLintFile file, boolean log) {
    return shouldAnalyze(file.getProjectRelativePath(), log);
  }

  public boolean shouldAnalyze(String relativePath, boolean log) {
    if (globalExclusions.test(relativePath)) {
      if (log) {
        SonarLintLogger.get().debug("File excluded from analysis due to configured global exclusions: " + relativePath);
      }
      return false;
    }

    if (projectExclusions.test(relativePath)) {
      if (log) {
        SonarLintLogger.get().debug("File excluded from analysis due to configured project exclusions: " + relativePath);
      }
      return false;
    }

    return true;
  }

  public static void addProjectFileExclusion(ISonarLintProject project, ISonarLintFile file, ExclusionItem exclusion) {
    SonarLintProjectConfiguration projectConfiguration = SonarLintProjectConfiguration.read(project.getScopeContext());
    List<ExclusionItem> fileExclusions = projectConfiguration.getFileExclusions();
    fileExclusions.add(exclusion);
    projectConfiguration.setFileExclusions(fileExclusions);
    projectConfiguration.save();
    SonarLintMarkerUpdater.clearMarkers(file);
  }

  public static boolean isPathAlreadyExcludedInProject(ISonarLintFile file) {
    ISonarLintProject project = file.getProject();
    String path = file.getProjectRelativePath();
    SonarLintProjectConfiguration projectConfiguration = SonarLintProjectConfiguration.read(project.getScopeContext());
    List<ExclusionItem> fileExclusions = projectConfiguration.getFileExclusions();
    return fileExclusions.stream().anyMatch(e -> e.type() == Type.FILE && path.equals(e.item()));
  }

  private static Set<String> getExclusionsOfType(Collection<ExclusionItem> exclusions, ExclusionItem.Type type) {
    return exclusions.stream()
      .filter(e -> e.type() == type)
      .map(ExclusionItem::item)
      .collect(Collectors.toSet());
  }
}
