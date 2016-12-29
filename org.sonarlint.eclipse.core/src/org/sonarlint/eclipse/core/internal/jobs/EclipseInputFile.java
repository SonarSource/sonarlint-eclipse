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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.internal.markers.TextFileContext;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

class EclipseInputFile implements ClientInputFile {
  private final List<PathMatcher> pathMatchersForTests;
  private final IFile file;
  private final Path filePath;
  private final String language;

  EclipseInputFile(List<PathMatcher> pathMatchersForTests, IFile file, Path filePath) {
    this(pathMatchersForTests, file, filePath, null);
  }
  
  EclipseInputFile(List<PathMatcher> pathMatchersForTests, IFile file, Path filePath, @Nullable String language) {
    this.pathMatchersForTests = pathMatchersForTests;
    this.file = file;
    this.filePath = filePath;
    this.language = language;
  }


  @Override
  public String getPath() {
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
      return null;
    }
  }

  @Override
  public <G> G getClientObject() {
    return (G) file;
  }

  @Override
  public String contents() throws IOException {
    try (TextFileContext context = new TextFileContext(file)) {
      return context.getDocument().get();
    } catch (CoreException e) {
      throw new IOException("error while reading file: " + file.getFullPath(), e);
    }
  }

  @Override
  public InputStream inputStream() throws IOException {
    Charset charset = getCharset();
    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }
    try (TextFileContext context = new TextFileContext(file)) {
      return new ByteArrayInputStream(contents().getBytes(charset));
    } catch (CoreException e) {
      throw new IOException("error while streaming file: " + file.getFullPath(), e);
    }
  }
}
