/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

/**
 * Two situations:
 *   - either a IDocument is provided, which mean the file is open in an editor
 *   - if document is <code>null</code> then file is not open but that doesn't mean we can read from FS, since the file might be stored on a remote FS
 *
 */
class EclipseInputFile implements ClientInputFile {
  private final boolean isTestFile;
  private final ISonarLintFile file;
  private final String language;
  private final IDocument editorDocument;
  private final Path tempDirectory;
  private Path filePath;

  EclipseInputFile(boolean isTestFile, ISonarLintFile file, Path tempDirectory, @Nullable IDocument editorDocument, @Nullable String language) {
    this.isTestFile = isTestFile;
    this.file = file;
    this.tempDirectory = tempDirectory;
    this.language = language;
    this.editorDocument = editorDocument;
  }

  @Override
  public String getPath() {
    if (filePath == null) {
      initFromFS(file, tempDirectory);
    }
    return filePath.toString();
  }

  private synchronized void initFromFS(ISonarLintFile file, Path temporaryDirectory) {
    IFileStore fileStore;
    try {
      fileStore = EFS.getStore(file.getResource().getLocationURI());
      File localFile = fileStore.toLocalFile(EFS.NONE, null);
      if (localFile == null) {
        // For analyzers to properly work we should ensure the temporary file has a "correct" name, and not a generated one
        localFile = new File(temporaryDirectory.toFile(), file.getProjectRelativePath());
        Files.createDirectories(localFile.getParentFile().toPath());
        fileStore.copy(EFS.getStore(localFile.toURI()), EFS.OVERWRITE, null);
      }
      filePath = localFile.toPath().toAbsolutePath();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to find path for file " + file, e);
    }
  }

  @Override
  public String relativePath() {
    return file.getProjectRelativePath();
  }

  @Override
  public boolean isTest() {
    return isTestFile;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public ISonarLintFile getClientObject() {
    return file;
  }

  @Override
  public URI uri() {
    return file.uri();
  }

  @Override
  public String contents() throws IOException {
    // Prefer to use editor Document when file is already opened in an editor
    if (editorDocument != null) {
      return editorDocument.get();
    }
    return file.getDocument().get();
  }

  @Override
  public InputStream inputStream() throws IOException {
    return new ByteArrayInputStream(contents().getBytes(getCharset()));
  }

}
