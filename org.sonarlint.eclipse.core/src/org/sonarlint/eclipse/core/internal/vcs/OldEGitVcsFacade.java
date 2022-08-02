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
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.vcs.GitUtils;

/**
 * Uses internal EGit
 * @deprecated Remove when we want to stop support of Eclipse with EGit < 5.12 (= Eclipse 2021-06)
 */
@Deprecated(forRemoval = true)
public class OldEGitVcsFacade implements VcsFacade {

  private static final SonarLintLogger LOG = SonarLintLogger.get();

  @Override
  public boolean isIgnored(ISonarLintFile file) {
    try {
      Class resourceStateFactoryClass = Class.forName("org.eclipse.egit.ui.internal.resources.ResourceStateFactory");
      Method getInstance = resourceStateFactoryClass.getMethod("getInstance");
      Object resourceStateFactory = getInstance.invoke(null);
      Method get = resourceStateFactoryClass.getMethod("get", IResource.class);
      Object resourceState = get.invoke(resourceStateFactory, file.getResource());
      Class resourceStateClass = Class.forName("org.eclipse.egit.ui.internal.resources.ResourceState");
      Method isIgnored = resourceStateClass.getMethod("isIgnored");
      return (boolean) isIgnored.invoke(resourceState);
    } catch (Exception e) {
      LOG.debug("Unable to call eGit", e);
      return false;
    }
  }

  @Override
  public Optional<String> electBestMatchingBranch(ISonarLintProject project, Set<String> serverCandidateNames, String serverMainBranch) {
    return withRepo(project.getResource(), repo -> GitUtils.electBestMatchingServerBranchForCurrentHead(repo, serverCandidateNames, serverMainBranch));
  }

  static <G> Optional<G> withRepo(IResource resource, Function<Repository, G> repoFunction) {
    RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
    if (mapping != null) {
      Repository repository = mapping.getRepository();
      if (repository != null) {
        return Optional.ofNullable(repoFunction.apply(repository));
      }
    }
    return Optional.empty();
  }

  @Override
  @Nullable
  public String getCurrentCommitRef(ISonarLintProject project) {
    return OldEGitVcsFacade.<String>withRepo(project.getResource(), repo -> {
      try {
        return Optional.ofNullable(repo.exactRef(Constants.HEAD)).map(Object::toString).orElse(null);
      } catch (IOException e) {
        LOG.debug("Unable to get current commit", e);
        return null;
      }
    }).orElse(null);
  }

}
