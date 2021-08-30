/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.resources;

import java.nio.charset.Charset;
import java.util.Objects;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.scm.GitUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class DefaultSonarLintFileAdapter implements ISonarLintFile {

  private final IFile file;
  private final ISonarLintProject project;

  public DefaultSonarLintFileAdapter(ISonarLintProject project, IFile file) {
    this.file = file;
    this.project = project;
  }

  @Override
  public String getName() {
    return file.getFullPath().toString();
  }

  @Override
  public ISonarLintProject getProject() {
    return project;
  }

  @Override
  public String getProjectRelativePath() {
    return file.getProjectRelativePath().toString();
  }

  @Override
  public IDocument getDocument() {
    ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
    IPath path = file.getFullPath();
    try {
      textFileBufferManager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
      ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path, LocationKind.IFILE);
      return textFileBuffer.getDocument();
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to open content of file " + file, e);
    } finally {
      try {
        textFileBufferManager.disconnect(path, LocationKind.IFILE, new NullProgressMonitor());
      } catch (CoreException e) {
        // Ignore
      }
    }
  }

  @Override
  public IResource getResource() {
    return file;
  }

  @Override
  public Charset getCharset() {
    try {
      return Charset.forName(file.getCharset());
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to determine charset of file " + file, e);
      return Charset.defaultCharset();
    }
  }

  @Override
  public boolean isScmIgnored() {
    // we only support git at the moment
    return isJGitPresent() && GitUtils.isIgnored(this);
  }

  private static boolean isJGitPresent() {
    try {
      Class.forName("org.eclipse.jgit.lib.Repository");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultSonarLintFileAdapter other = (DefaultSonarLintFileAdapter) obj;
    return Objects.equals(file, other.file);
  }

}
