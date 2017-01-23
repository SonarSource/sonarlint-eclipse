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
package org.sonarlint.eclipse.core.internal.jobs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

/**
 * Two situations:
 *   - either a IDocument is provided, which mean the file is open in an editor
 *   - if document is <code>null</code> then file is not open but that doesn't mean we can read from FS, since the file might be stored on a remote FS
 *
 */
class EclipseInputFile implements ClientInputFile {
  private final List<PathMatcher> pathMatchersForTests;
  private final IFile file;
  private final String language;
  private final IDocument document;
  private Path filePath;

  EclipseInputFile(List<PathMatcher> pathMatchersForTests, IFile file, @Nullable IDocument document) {
    this(pathMatchersForTests, file, document, null);
  }

  EclipseInputFile(List<PathMatcher> pathMatchersForTests, IFile file, @Nullable IDocument document, @Nullable String language) {
    this.pathMatchersForTests = pathMatchersForTests;
    this.file = file;
    this.language = language;
    this.document = document;
  }

  private synchronized void initFromFS() {
    IFileStore fileStore;
    try {
      fileStore = EFS.getStore(file.getLocationURI());
      File localFile = fileStore.toLocalFile(EFS.NONE, null);
      if (localFile == null) {
        // Try to get a cached copy (for virtual file systems)
        localFile = fileStore.toLocalFile(EFS.CACHE, null);
      }
      filePath = localFile.toPath().toAbsolutePath();
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to find path for file " + file, e);
    }
  }

  @Override
  public String getPath() {
    if (filePath == null) {
      initFromFS();
    }
    return filePath.toString();
  }

  @Override
  public boolean isTest() {
    for (PathMatcher matcher : pathMatchersForTests) {
      if (matcher.matches(filePath)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public Charset getCharset() {
    try {
      return Charset.forName(file.getCharset());
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to determine charset of file " + file, e);
    }
  }

  @Override
  public <G> G getClientObject() {
    return (G) file;
  }

  @Override
  public String contents() throws IOException {
    if (document != null) {
      return document.get();
    }
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    try (InputStream is = inputStream()) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = is.read(buffer)) != -1) {
        result.write(buffer, 0, length);
      }
    }
    return result.toString(getCharset().name());
  }

  @Override
  public InputStream inputStream() throws IOException {
    if (document != null) {
      Charset charset = getCharset();
      if (charset == null) {
        charset = StandardCharsets.UTF_8;
      }
      return new ByteArrayInputStream(contents().getBytes(charset));
    }
    try {
      return file.getContents(true);
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to read file content for " + file, e);
    }
  }

}
