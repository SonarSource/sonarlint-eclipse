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
package org.sonarlint.eclipse.core.internal.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class FileUtils {

  private FileUtils() {
    // Utility class
  }

  public static void deleteRecursively(Path dir) {
    try (var filesStream = Files.walk(dir)) {
      filesStream
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to delete directory: " + dir, e);
    }
  }

  public static void mkdirs(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create directory: " + path, e);
    }
  }

  /**
   *  Eclipse is using abstractions for the projects as well as the files and no direct file system access. In case of
   *  need this can be used to try to access the local file. Keep in mind that this might not work when resources are
   *  no local files.
   *
   *  @param resource that we want to try to get the local file from
   *  @return the actual file object if possible, null otherwise
   */
  @Nullable
  public static File toLocalFile(IResource resource) {
    try {
      var fileStore = EFS.getStore(resource.getLocationURI());
      return fileStore.toLocalFile(EFS.NONE, null);
    } catch (CoreException err) {
      SonarLintLogger.get().error("Error while trying to get local file of resource: " + resource.getName(), err);
    }
    return null;
  }
}
