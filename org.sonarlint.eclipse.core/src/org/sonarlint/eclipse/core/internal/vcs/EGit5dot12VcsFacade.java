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
package org.sonarlint.eclipse.core.internal.vcs;

import java.util.Optional;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.info.GitInfo;
import org.eclipse.jgit.lib.Repository;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * Uses the new EGit API
 * https://wiki.eclipse.org/EGit/New_and_Noteworthy/5.12#API
 *
 */
public class EGit5dot12VcsFacade extends AbstractEGitVcsFacade {

  @Override
  public boolean isIgnored(ISonarLintFile file) {
    try {
      var gitInfo = SonarLintUtils.adapt(file.getResource(), GitInfo.class,
        "[EGit5dot12VcsFacade#isIgnored] Try get GitInfo from file '" + file.getName() + "'");
      if (gitInfo == null) {
        return false;
      }
      return gitInfo.getGitState().isIgnored();
    } catch (NoClassDefFoundError notFound) {
      // SLE-785 Workaround for IDz
      return false;
    }
  }

  @Override
  Optional<Repository> getRepo(IResource resource) {
    try {
      var gitInfo = SonarLintUtils.adapt(resource, GitInfo.class,
        "[EGit5dot12VcsFacade#getRepo] Try get GitInfo from resource '" + resource + "'");
      return Optional.ofNullable(gitInfo).map(GitInfo::getRepository);
    } catch (NoClassDefFoundError notFound) {
      // SLE-785 Workaround for IDz
      return Optional.empty();
    }
  }

}
