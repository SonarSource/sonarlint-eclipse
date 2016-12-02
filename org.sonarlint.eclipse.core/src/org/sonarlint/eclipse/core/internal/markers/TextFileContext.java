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
package org.sonarlint.eclipse.core.internal.markers;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;

public class TextFileContext implements AutoCloseable {
  private final IPath path;
  private final ITextFileBufferManager textFileBufferManager;
  private final IDocument document;

  public TextFileContext(IResource file) throws CoreException {
    this.path = file.getFullPath();
    this.textFileBufferManager = FileBuffers.getTextFileBufferManager();
    textFileBufferManager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
    ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path, LocationKind.IFILE);
    document = textFileBuffer.getDocument();
  }

  @Override
  public void close() throws CoreException {
    textFileBufferManager.disconnect(path, LocationKind.IFILE, new NullProgressMonitor());
  }

  public IDocument getDocument() {
    return document;
  }
}
