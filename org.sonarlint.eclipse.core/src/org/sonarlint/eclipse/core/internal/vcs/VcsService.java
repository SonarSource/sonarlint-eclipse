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
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class VcsService {

  private static final Map<ISonarLintProject, Object> previousCommitRefCache = new ConcurrentHashMap<>();
  private static final Map<ISonarLintProject, Optional<String>> electedServerBranchCache = new ConcurrentHashMap<>();

  private VcsService() {
  }

  private static boolean isEGitPresent() {
    try {
      Class.forName("org.eclipse.egit.core.info.GitInfo");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static VcsFacade getFacade() {
    // For now we only support eGit
    if (isEGitPresent()) {
      return new EGitVcsFacade();
    }
    return new NoOpVcsFacade();
  }

  private static Optional<String> electBestMatchingBranch(ISonarLintProject project) {
    Optional<ResolvedBinding> bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(project);
    if (bindingOpt.isEmpty()) {
      return Optional.empty();
    }
    var serverBranches = bindingOpt.get().getEngineFacade().getServerBranches(bindingOpt.get().getProjectBinding().projectKey());

    Object previousCommitRef = previousCommitRefCache.get(project);
    VcsFacade facade = getFacade();
    Object newCommitRef = facade.getCurrentCommitRef(project);
    if (!Objects.equals(previousCommitRef, newCommitRef)) {
      // Current commit has changed, recompute the best elected branch
      electedServerBranchCache.remove(project);
      if (newCommitRef == null) {
        previousCommitRefCache.remove(project);
      } else {
        previousCommitRefCache.put(project, newCommitRef);
      }
    }

    return electedServerBranchCache.computeIfAbsent(project,
      p -> facade.electBestMatchingBranch(p, serverBranches.getBranchNames(), serverBranches.getMainBranchName().orElse(null)));
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
