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
package org.sonarlint.eclipse.core.internal.vcs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

import static java.util.stream.Collectors.joining;

public class VcsService {
  private static final SonarLintLogger LOG = SonarLintLogger.get();
  private static final JGitFacade FACADE = new JGitFacade();

  private static final Map<ISonarLintProject, Object> previousCommitRefCache = new ConcurrentHashMap<>();
  private static final Map<ISonarLintProject, String> matchedSonarProjectBranchCache = new ConcurrentHashMap<>();

  private VcsService() {
  }

  public static boolean isIgnored(ISonarLintFile file) {
    return FACADE.isIgnored(file);
  }

  public static boolean inRepository(IResource resource) {
    return FACADE.inRepository(resource);
  }

  @Nullable
  private static String electBestMatchingBranch(ISonarLintProject project, String mainBranchName, Set<String> allBranchesNames) {
    LOG.debug("Elect best matching branch for project '" + project.getName() + "' among: " + allBranchesNames.stream().collect(joining(",")));
    var matched = FACADE.electBestMatchingBranch(project, allBranchesNames, mainBranchName);
    LOG.debug("Best matching branch is: " + matched);
    return matched;
  }

  private static void saveCurrentCommitRef(ISonarLintProject project) {
    Object newCommitRef = FACADE.getCurrentCommitRef(project);
    if (newCommitRef == null) {
      previousCommitRefCache.remove(project);
    } else {
      previousCommitRefCache.put(project, newCommitRef);
    }
  }

  public static void projectClosed(ISonarLintProject project) {
    previousCommitRefCache.remove(project);
    matchedSonarProjectBranchCache.remove(project);
  }

  public static Optional<String> getCachedSonarProjectBranch(ISonarLintProject project) {
    return Optional.ofNullable(matchedSonarProjectBranchCache.get(project));
  }

  @Nullable
  public static String matchSonarProjectBranch(ISonarLintProject p, String mainBranchName, Set<String> allBranchesNames) {
    saveCurrentCommitRef(p);
    return electBestMatchingBranch(p, mainBranchName, allBranchesNames);
  }

  public static void installBranchChangeListener() {
    FACADE.addHeadRefsChangeListener(projects -> new BranchChangeJob(projects).schedule());
  }

  public static void removeBranchChangeListener() {
    FACADE.removeHeadRefsChangeListener();
  }

  private static class BranchChangeJob extends Job {
    private final List<ISonarLintProject> affectedProjects;

    public BranchChangeJob(List<ISonarLintProject> affectedProjects) {
      super("Refresh SonarLint matching branches");
      this.affectedProjects = affectedProjects;
      setPriority(LONG);
      setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      affectedProjects.forEach(project -> {
        Object newCommitRef = FACADE.getCurrentCommitRef(project);
        if (shouldRecomputeMatchingBranch(project, newCommitRef)) {
          saveCurrentCommitRef(project);
          SonarLintBackendService.get().didVcsRepositoryChange(project);
        }
      });
      return Status.OK_STATUS;
    }

    private static boolean shouldRecomputeMatchingBranch(ISonarLintProject project, @Nullable Object newCommitRef) {
      var previousCommitRef = previousCommitRefCache.get(project);
      if (!Objects.equals(previousCommitRef, newCommitRef)) {
        LOG.debug("HEAD has changed since last election, evict cached branch...");
        return true;
      }
      return false;
    }
  }

  public static void updateCachedMatchedSonarProjectBranch(ISonarLintProject project, String newMatchedBranchName) {
    matchedSonarProjectBranchCache.put(project, newMatchedBranchName);
  }
}
