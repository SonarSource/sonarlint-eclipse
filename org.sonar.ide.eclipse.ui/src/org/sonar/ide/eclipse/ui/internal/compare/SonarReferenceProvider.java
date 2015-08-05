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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.wsclient.ConnectionException;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

public class SonarReferenceProvider implements IQuickDiffReferenceProvider {

  private String id;
  private IDocument source;
  private IResource resource;

  @Override
  public void dispose() {
    source = null;
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
    if (source != null) {
      return source;
    }
    if (resource != null) {
      String remoteSrc = downloadRemoteSource(resource);
      if (remoteSrc != null) {
        source = new Document(remoteSrc);
      }
    }
    return source;
  }

  static String downloadRemoteSource(IResource resource) {
    if (resource.getAdapter(IFile.class) == null) {
      return null;
    }
    IProject project = resource.getProject();
    if (!SonarNature.hasSonarNature(project)) {
      return null;
    }
    SonarProject sonarProject = SonarProject.getInstance(project);
    SonarServer sonarServer = (SonarServer) SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (sonarServer == null) {
      SonarCorePlugin.getDefault().error(NLS.bind(Messages.No_matching_server_in_configuration_for_project,
        sonarProject.getProject().getName(), sonarProject.getUrl()) + "\n");
      return null;
    }
    if (sonarServer.disabled()) {
      SonarCorePlugin.getDefault().info("SonarQube server is disabled for project " + sonarProject.getProject().getName() + " \n");
      return null;
    }
    ISonarResource element = ResourceUtils.adapt(resource);
    if (element == null) {
      return null;
    }
    try {
      String[] remoteSrc = WSClientFactory.getSonarClient(sonarServer).getRemoteCode(element.getKey());
      if (remoteSrc != null) {
        return StringUtils.join(remoteSrc, "\n");
      }
    } catch (ConnectionException e) {
      return null;
    }
    return null;
  }

  @Override
  public void setActiveEditor(final ITextEditor targetEditor) {
    source = null;
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
