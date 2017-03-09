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
package org.sonarlint.eclipse.core.resource;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;

public interface ISonarLintFile extends ISonarLintIssuable {

  /**
   * The IFile resource that will be found in editor.
   * TODO drop that
   */
  IFile getFileInEditor();

  /**
   * Return a document that represents content of this file.
   */
  IDocument getDocument();

  /**
   * Path of the file inside its project. Should use same separator than SonarQube keys ('/').
   */
  String getProjectRelativePath();

  /**
   * Charset to be used to read this file content.
   */
  Charset getCharset();

  /**
   * Return an InputStream on the file content.
   */
  InputStream inputStream();

  /**
   * A physical path to a file with same content than this file.
   * Filename should be the same as the original file.
   * Location should be relative to workspace (so don't create a temp file in Operating System temp folder).
   * TODO will be removed when analyzers are able to rely only on {@link #inputStream()} and {@link #getProjectRelativePath()}
   * @param tempDirectory if needed, use this temporary directory to create a temporary file. The directory is cleaned after analysis.
   */
  String getPhysicalPath(Path tempDirectory);

  /**
   * The underlying IFile if applicable. Used by some configurators to get some details.
   */
  @CheckForNull
  IFile getUnderlyingFile();

}
