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

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.TriggerType;

public class AnalyzeProjectRequest {

  private final IProject project;
  private final Collection<FileWithDocument> files;
  private final TriggerType triggerType;

  public static class FileWithDocument {
    private final IFile file;
    private final IDocument document;

    public FileWithDocument(IFile file, @Nullable IDocument document) {
      this.file = file;
      this.document = document;
    }

    public IFile getFile() {
      return file;
    }

    @CheckForNull
    public IDocument getDocument() {
      return document;
    }

  }

  public AnalyzeProjectRequest(IProject project, Collection<FileWithDocument> files, TriggerType triggerType) {
    this.project = project;
    this.files = files;
    this.triggerType = triggerType;
  }

  public IProject getProject() {
    return project;
  }

  public Collection<FileWithDocument> getFiles() {
    return files;
  }

  public TriggerType getTriggerType() {
    return triggerType;
  }

}
