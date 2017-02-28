/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.Nullable;
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
  private final List<PathMatcher> pathMatchersForTests;
  private final ISonarLintFile file;
  private final String language;
  private final IDocument document;
  private final Path tempDirectory;

  EclipseInputFile(List<PathMatcher> pathMatchersForTests, ISonarLintFile file, Path tempDirectory, @Nullable IDocument document, @Nullable String language) {
    this.pathMatchersForTests = pathMatchersForTests;
    this.file = file;
    this.tempDirectory = tempDirectory;
    this.language = language;
    this.document = document;
  }

  @Override
  public String getPath() {
    return file.getPhysicalPath(tempDirectory);
  }

  @Override
  public boolean isTest() {
    Path absolutePath = Paths.get(file.getProject().getName()).resolve(file.getProjectRelativePath());
    for (PathMatcher matcher : pathMatchersForTests) {
      if (matcher.matches(absolutePath)) {
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
    return file.getCharset();
  }

  @Override
  public <G> G getClientObject() {
    return (G) file;
  }

  @Override
  public String contents() throws IOException {
    // Prefer to use Document when file is already opened in an editor
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
    // Prefer to use Document when file is already opened in an editor
    if (document != null) {
      Charset charset = getCharset();
      if (charset == null) {
        charset = StandardCharsets.UTF_8;
      }
      return new ByteArrayInputStream(contents().getBytes(charset));
    }
    return file.inputStream();
  }

}
