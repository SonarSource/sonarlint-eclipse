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
package org.sonarlint.eclipse.core.internal.vcs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

import static java.util.stream.Collectors.joining;

public class VcsService {

  private static final SonarLintLogger LOG = SonarLintLogger.get();

  public static final boolean IS_EGIT_5_12_BUNDLE_AVAILABLE;
  static {
    var result = false;
    try {
      var egitBundle = Platform.getBundle("org.eclipse.egit.core");
      result = egitBundle != null && (egitBundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED)) != 0
        && egitBundle.getVersion().compareTo(new Version(5, 12, 0)) >= 0;
    } catch (Throwable exception) {
      // Assume that it's not available.
    }
    IS_EGIT_5_12_BUNDLE_AVAILABLE = result;
  }

  public static final boolean IS_EGIT_UI_BUNDLE_AVAILABLE;
  static {
    var result = false;
    try {
      var egitUiBundle = Platform.getBundle("org.eclipse.egit.ui");
      result = egitUiBundle != null && (egitUiBundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED)) != 0;
    } catch (Throwable exception) {
      // Assume that it's not available.
    }
    IS_EGIT_UI_BUNDLE_AVAILABLE = result;
  }

  private static final Map<ISonarLintProject, Object> previousCommitRefCache = new ConcurrentHashMap<>();
  private static final Map<ISonarLintProject, Optional<String>> electedServerBranchCache = new ConcurrentHashMap<>();

  private VcsService() {
  }

  public static VcsFacade getFacade() {
    // For now we only support eGit
    if (IS_EGIT_5_12_BUNDLE_AVAILABLE) {
      return new EGit5dot12VcsFacade();
    }
    if (IS_EGIT_UI_BUNDLE_AVAILABLE) {
      return new OldEGitVcsFacade();
    }
    return new NoOpVcsFacade();
  }

  private static Optional<String> electBestMatchingBranch(ISonarLintProject project) {
    LOG.debug("Elect best matching branch for project " + project.getName() + "...");
    Optional<ResolvedBinding> bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(project);
    if (bindingOpt.isEmpty()) {
      LOG.debug("Project not bound");
      return Optional.empty();
    }

    Object previousCommitRef = previousCommitRefCache.get(project);
    VcsFacade facade = getFacade();
    Object newCommitRef = facade.getCurrentCommitRef(project);
    if (!Objects.equals(previousCommitRef, newCommitRef)) {
      LOG.debug("HEAD has changed since last election, evict cached branch...");
      electedServerBranchCache.remove(project);
      if (newCommitRef == null) {
        previousCommitRefCache.remove(project);
      } else {
        previousCommitRefCache.put(project, newCommitRef);
      }
    }

    var branch = electedServerBranchCache.computeIfAbsent(project,
      p -> {
        var serverBranches = bindingOpt.get().getEngineFacade().getServerBranches(bindingOpt.get().getProjectBinding().projectKey());
        LOG.debug("Find best matching branch among: " + serverBranches.getBranchNames().stream().collect(joining(",")));
        return facade.electBestMatchingBranch(p, serverBranches.getBranchNames(), serverBranches.getMainBranchName().orElse(null));
      });
    LOG.debug("Best matching branch is " + branch.orElse("<main>"));
    return branch;
  }

  public static void projectClosed(ISonarLintProject project) {
    previousCommitRefCache.remove(project);
    electedServerBranchCache.remove(project);
  }

  public static void clearVcsCache() {
    previousCommitRefCache.clear();
    electedServerBranchCache.clear();
  }

  @Nullable
  public static String getServerBranch(ISonarLintProject project) {
    return VcsService.electBestMatchingBranch(project).orElse(null);
  }

}
