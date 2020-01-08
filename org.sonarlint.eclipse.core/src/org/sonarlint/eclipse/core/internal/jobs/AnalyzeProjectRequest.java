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

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class AnalyzeProjectRequest {

  private final ISonarLintProject project;
  private final Collection<FileWithDocument> files;
  private final TriggerType triggerType;
  private final boolean shouldClearReport;

  public static class FileWithDocument {
    private final ISonarLintFile file;
    private final IDocument document;

    public FileWithDocument(ISonarLintFile file, @Nullable IDocument document) {
      this.file = file;
      this.document = document;
    }

    public ISonarLintFile getFile() {
      return file;
    }

    @CheckForNull
    public IDocument getDocument() {
      return document;
    }

  }

  public AnalyzeProjectRequest(ISonarLintProject project, Collection<FileWithDocument> files, TriggerType triggerType, boolean shouldClearReport) {
    this.project = project;
    this.triggerType = triggerType;
    this.files = files;
    this.shouldClearReport = shouldClearReport;
  }

  public Collection<FileWithDocument> getFiles() {
    return files;
  }

  public AnalyzeProjectRequest(ISonarLintProject project, Collection<FileWithDocument> files, TriggerType triggerType) {
    this(project, files, triggerType, false);
  }

  public ISonarLintProject getProject() {
    return project;
  }

  public TriggerType getTriggerType() {
    return triggerType;
  }

  public boolean shouldClearReport() {
    return shouldClearReport;
  }

}
