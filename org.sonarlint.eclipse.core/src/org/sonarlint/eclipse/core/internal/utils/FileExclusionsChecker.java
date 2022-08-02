/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.jobs.TestFileClassifier;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.ClientFileExclusions;
import org.sonarsource.sonarlint.core.commons.SonarLintException;

import static java.util.stream.Collectors.toCollection;

public class FileExclusionsChecker {
  private final ClientFileExclusions projectExclusions;
  private final ClientFileExclusions globalExclusions;

  public FileExclusionsChecker(ISonarLintProject project) {
    var projectConfiguration = SonarLintCorePlugin.loadConfig(project);
    var globalExclusionItems = SonarLintGlobalConfiguration.getGlobalExclusions();
    var projectExclusionItems = projectConfiguration.getFileExclusions();

    var projectFileExclusions = getExclusionsOfType(projectExclusionItems, Type.FILE);
    var projectDirectoryExclusions = getExclusionsOfType(projectExclusionItems, Type.DIRECTORY);
    var projectGlobExclusions = getExclusionsOfType(projectExclusionItems, Type.GLOB);
    var globalGlobExclusions = getExclusionsOfType(globalExclusionItems, Type.GLOB);

    projectExclusions = new ClientFileExclusions(projectFileExclusions, projectDirectoryExclusions, projectGlobExclusions);
    globalExclusions = new ClientFileExclusions(Collections.emptySet(), Collections.emptySet(), globalGlobExclusions);
  }

  public Collection<ISonarLintFile> filterExcludedFiles(ISonarLintProject project, Collection<ISonarLintFile> files) {
    return filterExcludedFiles(project, files, true);
  }

  public Collection<ISonarLintFile> filterExcludedFiles(ISonarLintProject project, Collection<ISonarLintFile> files, boolean log) {
    var notExcluded = files
      .stream()
      .filter(file -> !isExcludedByLocalConfiguration(file, log))
      .collect(toCollection(HashSet::new));

    SonarLintCorePlugin.getServersManager()
      .resolveBinding(project)
      .ifPresent(binding -> {
        var testFileClassifier = TestFileClassifier.get();
        try {
          var excludedByServerSideExclusions = binding.getEngineFacade().getServerFileExclusions(binding.getProjectBinding(), notExcluded,
            testFileClassifier::isTest);
          notExcluded.removeAll(excludedByServerSideExclusions);
          excludedByServerSideExclusions.forEach(file -> {
            notExcluded.remove(file);
            logIfNeeded(file, log, "server side");
          });
        } catch (SonarLintException e) {
          SonarLintLogger.get().error("Unable to read server side exclusions. Check your binding.", e);
        }
      });
    return notExcluded;
  }

  public boolean isExcluded(ISonarLintFile file, boolean log) {
    return filterExcludedFiles(file.getProject(), List.of(file), log).isEmpty();
  }

  private boolean isExcludedByLocalConfiguration(ISonarLintFile file, boolean log) {
    var relativePath = file.getProjectRelativePath();

    if (globalExclusions.test(relativePath)) {
      logIfNeeded(file, log, "global");
      return true;
    }

    if (projectExclusions.test(relativePath)) {
      logIfNeeded(file, log, "project");
      return true;
    }

    return false;
  }

  private static void logIfNeeded(ISonarLintFile file, boolean log, String exclusionSource) {
    if (log) {
      SonarLintLogger.get().debug("File '" + file.getName() + "' excluded from analysis due to configured " + exclusionSource + " exclusions");
    }
  }

  public static void addProjectFileExclusion(ISonarLintProject project, ISonarLintFile file, ExclusionItem exclusion) {
    var projectConfiguration = SonarLintCorePlugin.loadConfig(project);
    projectConfiguration.getFileExclusions().add(exclusion);
    SonarLintCorePlugin.saveConfig(project, projectConfiguration);
    SonarLintMarkerUpdater.clearMarkers(file);
  }

  public static boolean isPathAlreadyExcludedInProject(ISonarLintFile file) {
    var project = file.getProject();
    var path = file.getProjectRelativePath();
    var projectConfiguration = SonarLintCorePlugin.loadConfig(project);
    var fileExclusions = projectConfiguration.getFileExclusions();
    return fileExclusions.stream().anyMatch(e -> e.type() == Type.FILE && path.equals(e.item()));
  }

  private static Set<String> getExclusionsOfType(Collection<ExclusionItem> exclusions, ExclusionItem.Type type) {
    return exclusions.stream()
      .filter(e -> e.type() == type)
      .map(ExclusionItem::item)
      .collect(Collectors.toSet());
  }
}
