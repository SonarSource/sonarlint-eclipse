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

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.info.GitInfo;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.vcs.GitUtils;

public class EGitVcsFacade implements VcsFacade {

  private static final SonarLintLogger LOG = SonarLintLogger.get();

  @Override
  public boolean isIgnored(ISonarLintFile file) {
    GitInfo gitInfo = Adapters.adapt(file.getResource(), GitInfo.class);
    if (gitInfo == null) {
      return false;
    }
    return gitInfo.getGitState().isIgnored();
  }

  @Override
  public Optional<String> electBestMatchingBranch(ISonarLintProject project, Set<String> serverCandidateNames, @Nullable String serverMainBranch) {
    LOG.debug("Compute best matching server branch for project " + project.getName());
    return withRepo(project.getResource(), repo -> GitUtils.electBestMatchingServerBranchForCurrentHead(repo, serverCandidateNames, serverMainBranch));
  }

  static <G> Optional<G> withRepo(IResource resource, Function<Repository, G> repoFunction) {
    GitInfo gitInfo = Adapters.adapt(resource, GitInfo.class);
    if (gitInfo != null) {
      Repository repository = gitInfo.getRepository();
      if (repository != null) {
        return Optional.ofNullable(repoFunction.apply(repository));
      }
    }
    return Optional.empty();
  }

  @Override
  @Nullable
  public String getCurrentCommitRef(ISonarLintProject project) {
    return EGitVcsFacade.<String>withRepo(project.getResource(), repo -> {
      try {
        return Optional.ofNullable(repo.exactRef(Constants.HEAD)).map(r -> r.toString()).orElse(null);
      } catch (IOException e) {
        LOG.debug("Unable to get current commit", e);
        return null;
      }
    }).orElse(null);
  }

}
