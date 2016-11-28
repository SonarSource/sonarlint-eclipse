/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.cdt.internal;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class FilePathResolver {
  public Path getPath(IFile file) throws CoreException {
    IFileStore fileStore = EFS.getStore(file.getLocationURI());
    File localFile = fileStore.toLocalFile(EFS.NONE, null);
    if (localFile == null) {
      // Try to get a cached copy (for virtual file systems)
      localFile = fileStore.toLocalFile(EFS.CACHE, null);
    }
    return localFile.toPath().toAbsolutePath();
  }
  
  public Path getWorkDir() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").toFile().toPath();
  }
}
