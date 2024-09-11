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
package org.sonarlint.eclipse.core.internal.jobs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * Two situations:
 *   - either a IDocument is provided, which mean the file is open in an editor
 *   - if document is <code>null</code> then file is not open but that doesn't mean we can read from FS, since the file might be stored on a remote FS
 *
 */
class EclipseInputFile {
  private final ISonarLintFile file;
  @Nullable
  private final IDocument editorDocument;
  private final Path tempDirectory;
  @Nullable
  private Path filePath;
  private final long documentModificationStamp;

  EclipseInputFile(ISonarLintFile file, Path tempDirectory, @Nullable IDocument editorDocument) {
    this.file = file;
    this.tempDirectory = tempDirectory;
    this.editorDocument = editorDocument;
    this.documentModificationStamp = editorDocument != null ? ((IDocumentExtension4) editorDocument).getModificationStamp() : 0;
  }

  public ISonarLintFile getFile() {
    return file;
  }

  public String getPath() {
    if (filePath == null) {
      initFromFS(file, tempDirectory);
    }
    return filePath.toString();
  }

  private synchronized void initFromFS(ISonarLintFile file, Path temporaryDirectory) {
    try {
      var fileStore = EFS.getStore(file.getResource().getLocationURI());
      var localFile = fileStore.toLocalFile(EFS.NONE, null);
      if (localFile == null) {
        // For analyzers to properly work we should ensure the temporary file has a "correct" name, and not a generated one
        localFile = new File(temporaryDirectory.toFile(), file.getProjectRelativePath());
        Files.createDirectories(localFile.getParentFile().toPath());
        fileStore.copy(EFS.getStore(localFile.toURI()), EFS.OVERWRITE, null);
      }
      filePath = localFile.toPath().toRealPath();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to find path for file " + file, e);
    }
  }

  public boolean hasDocumentOlderThan(IDocument document) {
    return editorDocument != null && documentModificationStamp < ((IDocumentExtension4) document).getModificationStamp();
  }

}
