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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintUtilsLogOutput;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.utils.GitUtils;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.events.ListenerHandle;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.ignore.IgnoreNode;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.Constants;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.ObjectLoader;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.Ref;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.lib.Repository;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.revwalk.RevWalk;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.treewalk.TreeWalk;
import org.sonarsource.sonarlint.shaded.org.eclipse.jgit.treewalk.filter.PathFilter;

/**
 *  Facade that only relies on the shaded JGit version coming from SonarLint CORE
 */
public class JGitFacade {
  private static final SonarLintLogger LOG = SonarLintLogger.get();

  private ListenerHandle listenerHandle;

  public synchronized void addHeadRefsChangeListener(Consumer<List<ISonarLintProject>> listener) {
    removeHeadRefsChangeListener();
    listenerHandle = Repository.getGlobalListenerList().addRefsChangedListener(event -> {
      List<ISonarLintProject> affectedProjects = new ArrayList<>();
      SonarLintUtils.allProjects().forEach(p -> getRepo(p.getResource()).ifPresent(repo -> {
        var repoDir = repo.getDirectory();
        if (repoDir != null && repoDir.equals(event.getRepository().getDirectory())) {
          affectedProjects.add(p);
        }
      }));
      if (!affectedProjects.isEmpty()) {
        listener.accept(affectedProjects);
      }
    });
  }

  public synchronized void removeHeadRefsChangeListener() {
    if (listenerHandle != null) {
      listenerHandle.remove();
    }
  }

  /**
   *  We want to check if a specific resource, e.g. a file/folder/project, is inside a repository.
   *  The {@link org.eclipse.core.resources.IResource} should be coming from the
   *  {@link org.sonarlint.eclipse.core.resource.ISonarLintIssuable#getResource()}!
   */
  public boolean inRepository(IResource resource) {
    return getRepo(resource).isPresent();
  }

  public String electBestMatchingBranch(ISonarLintProject project, Set<String> serverCandidateNames, String serverMainBranch) {
    return getRepo(project.getResource())
      .map(repo -> GitUtils.electBestMatchingServerBranchForCurrentHead(repo, serverCandidateNames, serverMainBranch, new SonarLintUtilsLogOutput()))
      .orElse(serverMainBranch);
  }

  @Nullable
  public String getCurrentCommitRef(ISonarLintProject project) {
    var repoOpt = getRepo(project.getResource());
    return repoOpt.isEmpty()
      ? null
      : getHeadRef(repoOpt.get());
  }

  @Nullable
  private String getHeadRef(Repository repo) {
    try {
      return Optional.ofNullable(repo.exactRef(Constants.HEAD)).map(Ref::toString).orElse(null);
    } catch (IOException e) {
      LOG.debug("Unable to get current commit", e);
      return null;
    }
  }

  /** Assuming this resource provided is the main project resource */
  private static Optional<Repository> getRepo(IResource resource) {
    try {
      var resourceRealPath = new File(resource.getLocationURI()).toPath().toRealPath();
      var repo = GitUtils.getRepositoryForDir(resourceRealPath, new SonarLintUtilsLogOutput());
      return repo == null ? Optional.empty() : Optional.of(repo);
    } catch (IOException err) {
      LOG.debug("Unable to get real path of resource: " + resource.getName(), err);
    } catch (IllegalStateException err) {
      LOG.debug("Unable to get Git repository for resource: " + resource.getName(), err);
    } catch (IllegalArgumentException err) {
      // This happens for all URI schemes that are not "file", like "rse" which is coming from the Eclipse Remote
      // System Explorer plug-in. Before these changes it was failing internally in the EGit integration as well and
      // would not return a repository as it is remote.
      LOG.debug("Unable to create file from resource: " + resource.getName(), err);
    }

    return Optional.empty();
  }

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
      LOG.debug("Unable to get real path of resource: " + fileResource.getName()
        + " or its project: " + projectResource.getName(), err);
      return false;
    } catch (IllegalArgumentException err) {
      // This happens for all URI schemes that are not "file", like "rse" which is coming from the Eclipse Remote
      // System Explorer plug-in. Before these changes it was failing internally in the EGit integration as well and
      // would therefore not be able to check if this file is ignored or not.
      LOG.debug("Unable to create file from resource: " + fileResource.getName(), err);
      return false;
    }

    var ignores = getGitignoreEntries(repo);
    LOG.debug("For project '" + projectResource.getName() + "' the ignore rules found by Git are: "
      + ignores.toString());

    // Because the return value can be null we have to check it this way!
    var isIgnored = ignores.checkIgnored(projectRealPath.relativize(fileRealPath).toString(),
      file.getResource().getType() == IResource.FOLDER);
    return isIgnored == null ? false : isIgnored;
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
      LOG.debug("Cannot load ignored resources for the Git repository", err);
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
