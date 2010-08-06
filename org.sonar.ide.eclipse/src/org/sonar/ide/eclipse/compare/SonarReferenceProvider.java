/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.compare;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.Source;

/**
 * @author Jérémie Lagarde
 */
public class SonarReferenceProvider implements IQuickDiffReferenceProvider {

  private String id;
  private IDocument sonarSource;
  private IResource resource;

  public void dispose() {
    sonarSource = null;
    resource = null;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public IDocument getReference(final IProgressMonitor monitor) throws CoreException {
    if (sonarSource != null) {
      return sonarSource;
    }
    if (resource != null) {
      final Source source = EclipseSonar.getInstance(resource.getProject()).search(resource).getCode();
      // TODO : do it in Source object : Source.toString()
      // sonarSource = new Document(source.toString());
      final StringBuilder stringBuilder = new StringBuilder();
      for (final String line : source.getLines()) {
        stringBuilder.append(line).append("\n");
      }
      sonarSource = new Document(stringBuilder.toString());
    }
    return sonarSource;
  }

  public void setActiveEditor(final ITextEditor targetEditor) {
    sonarSource = null;
    resource = null;
    if (targetEditor != null) {
      resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
    }
  }

  public boolean isEnabled() {
    return resource != null;
  }

  protected Host getSonarHost(final IProject project) {
    final ProjectProperties properties = ProjectProperties.getInstance(project);
    return SonarPlugin.getServerManager().createServer(properties.getUrl());
  }

}
