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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.core.resources.IResource;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintUtilsLogOutput;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.ignore.IgnoreNode;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.Constants;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.ObjectLoader;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.Repository;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.revwalk.RevWalk;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.treewalk.TreeWalk;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.treewalk.filter.PathFilter;

/**
 *  Facade that only relies on the shaded JGit version coming from SonarLint CORE
 */
public class JGitVcsFacade extends AbstractEGitVcsFacade {
  /** Assuming this resource provided is the main project resource */
  @Override
  Optional<Repository> getRepo(IResource resource) {

    try {
      var resourceRealPath = new File(resource.getLocationURI()).toPath().toRealPath();
      var repo = org.sonarsource.sonarlint.core.client.utils.GitUtils.getRepositoryForDir(resourceRealPath, new SonarLintUtilsLogOutput());
      return repo == null ? Optional.empty() : Optional.of(repo);
    } catch (IOException err) {
      SonarLintLogger.get().debug("Unable to get real path of resource: " + resource.getName(), err);
    } catch (IllegalStateException err) {
      SonarLintLogger.get().debug("Unable to get Git repository for resource: " + resource.getName(), err);
    }

    return Optional.empty();
  }

  @Override
  public boolean isIgnored(ISonarLintFile file) {
    var repoOpt = getRepo(file.getProject().getResource());
    if (repoOpt.isEmpty()) {
      return false;
    }
    var repo = repoOpt.get();

    var fileResource = file.getResource();
    var projectResource = file.getProject().getResource();

    Path fileRealPath;
    Path projectRealPath;
    try {
      fileRealPath = new File(fileResource.getLocationURI()).toPath().toRealPath();
      projectRealPath = new File(projectResource.getLocationURI()).toPath().toRealPath();
    } catch (IOException err) {
      SonarLintLogger.get().debug("Unable to get real path of resource: " + fileResource.getName()
        + " or its project: " + projectResource.getName(), err);
      return false;
    }

    var ignores = getGitignoreEntries(repo);
    SonarLintLogger.get().debug("For project '" + projectResource.getName() + "' the ignore rules found by Git are: "
      + ignores.toString());

    // Because the return value can be null we have to check it this way!
    var isIgnored = ignores.checkIgnored(projectRealPath.relativize(fileRealPath).toString(),
      file.getResource().getType() == IResource.FOLDER);
    return Boolean.TRUE.equals(isIgnored);
  }

  /**
   *  Get all the ignore rules for a specific Git repository
   *
   *  @param repository to check for ".gitignore" files
   *  @return ignore rules, can be empty
   */
  private static IgnoreNode getGitignoreEntries(Repository repository) {
    var ignoreNode = new IgnoreNode();
    try {
      if (repository.isBare()) {
        handleBareRepo(repository, ignoreNode);
      } else {
        handleNonBareRepo(repository, ignoreNode);
      }
    } catch (IOException err) {
      SonarLintLogger.get().debug("Cannot load ignored resources for the Git repository", err);
    }
    return ignoreNode;
  }

  /** Handle non-bare repositories where the root directory can be accessed quite easily */
  private static void handleNonBareRepo(Repository repository, IgnoreNode ignoreNode) throws IOException {
    var rootDir = repository.getWorkTree();
    var gitIgnoreFile = new File(rootDir, Constants.GITIGNORE_FILENAME);
    try (var inputStream = new FileInputStream(gitIgnoreFile)) {
      ignoreNode.parse(inputStream);
    }
  }

  /** Handle bare repositories where the ".gitignore" can only be accessed by walking the tree */
  private static void handleBareRepo(Repository repository, IgnoreNode ignoreNode) throws IOException {
    var loader = readBareGitignore(repository);
    if (loader.isPresent()) {
      try (var inputStream = loader.get().openStream()) {
        ignoreNode.parse(inputStream);
      }
    }
  }

  private static Optional<ObjectLoader> readBareGitignore(Repository repository) throws IOException {
    var headId = repository.resolve(Constants.HEAD);

    try (var revWalk = new RevWalk(repository)) {
      var commit = revWalk.parseCommit(headId);

      try (var treeWalk = new TreeWalk(repository)) {
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(Constants.GITIGNORE_FILENAME));
        return !treeWalk.next()
          ? Optional.empty()
          : Optional.of(repository.open(treeWalk.getObjectId(0)));
      }
    }
  }
}
