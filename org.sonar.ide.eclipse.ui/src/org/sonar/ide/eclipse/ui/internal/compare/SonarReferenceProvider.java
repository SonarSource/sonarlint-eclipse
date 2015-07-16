/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.compare;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.remote.EclipseSonar;
import org.sonar.ide.eclipse.core.internal.remote.SourceCode;

public class SonarReferenceProvider implements IQuickDiffReferenceProvider {

  private String id;
  private IDocument sonarSource;
  private IResource resource;

  @Override
  public void dispose() {
    sonarSource = null;
    resource = null;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public IDocument getReference(final IProgressMonitor monitor) throws CoreException {
    if (sonarSource != null) {
      return sonarSource;
    }
    if (resource != null) {
      IProject project = resource.getProject();
      if (!SonarNature.hasSonarNature(project)) {
        return null;
      }
      EclipseSonar eclipseSonar = EclipseSonar.getInstance(project);
      if (eclipseSonar == null) {
        return null;
      }
      SourceCode sourceCode = eclipseSonar.search(resource);
      if (sourceCode != null) {
        sonarSource = new Document(sourceCode.getRemoteContent());
      }
    }
    return sonarSource;
  }

  @Override
  public void setActiveEditor(final ITextEditor targetEditor) {
    sonarSource = null;
    resource = null;
    if (targetEditor != null) {
      resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
    }
  }

  @Override
  public boolean isEnabled() {
    return resource != null;
  }

}
