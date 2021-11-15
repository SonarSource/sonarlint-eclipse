/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.scm;

import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class GitUtils {

  public static boolean isIgnored(ISonarLintFile file) {
    var builder = new FileRepositoryBuilder();
    var fileToCheckLocation = file.getResource().getLocation();
    if (fileToCheckLocation == null) {
      return false;
    }
    var fileToCheck = fileToCheckLocation.toFile();
    try (var repository = builder.findGitDir(fileToCheck.getParentFile()).build()) {
      if (repository.getObjectDatabase().exists()) {
        var repositoryRootPath = repository.getWorkTree().toPath();
        try (var treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(new FileTreeIterator(repository));
          treeWalk.setRecursive(true);
          treeWalk.setFilter(PathFilter.create(repositoryRootPath.relativize(fileToCheck.toPath()).toString()));
          // the iterator that we provide in addTree might have been replaced when walking subfolders
          return treeWalk.next() && treeWalk.getTree(FileTreeIterator.class).isEntryIgnored();
        }
      }
    } catch (Exception e) {
      // consider file to not be ignored
    }
    return false;
  }

  private GitUtils() {
    // utility class
  }
}
