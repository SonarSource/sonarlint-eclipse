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
package org.sonarlint.eclipse.core.resource;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.jface.text.IDocument;

/**
 * Represents a file that can be analyzed for SonarLint.
 * Should implement hashCode and equals.
 * @since 3.0
 */
public interface ISonarLintFile extends ISonarLintIssuable {

  /**
   * Return a document that represents content of this file.
   */
  IDocument getDocument();

  /**
   * Path of the file inside its project. Should use same separator than SonarQube keys ('/').
   * Used by issue tracking.
   */
  String getProjectRelativePath();

  /**
   * Some analyzers need to read the file from disk so {@link #getDocument()} will not be used, and instead
   * {@link EFS} will be queried for a physical copy of the file, and content will be read using this charset.
   *
   * @throws UnsupportedCharsetException if the JVM does not offer support for the named charset
   * @throws UnsupportedEncodingException if the JVM specific XML loader does not support the encoding
   */
  Charset getCharset();

  /**
   * Unique URI to identify this file
   * @since 3.3
   */
  default URI uri() {
    return getResource().getLocationURI();
  }

  /**
   * @return true if the file is ignored by the underlying SCM provider (e.g. through the .gitignore file for Git), else false
   */
  default boolean isScmIgnored() {
    return false;
  }

}
