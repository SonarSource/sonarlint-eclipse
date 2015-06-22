/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.ui.internal.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.EditorPart;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;
import org.sonar.ide.eclipse.ui.internal.util.SelectionUtils;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * Display details of a project or Sonar resource in a web browser
 * @author Evgeny Mandrikov
 */
public class ResourceWebView extends AbstractLinkedSonarWebView<ISonarResource>implements ISelectionListener {

  public static final String ID = SonarUiPlugin.PLUGIN_ID + ".views.ResourceWebView";

  @Override
  protected ISonarResource findSelectedElement(IWorkbenchPart part, ISelection selection) {
    if (part instanceof EditorPart) {
      EditorPart editor = (EditorPart) part;
      IEditorInput editorInput = editor.getEditorInput();
      IResource resource = ResourceUtil.getResource(editorInput);
      return ResourceUtils.adapt(resource);
    } else if (selection instanceof IStructuredSelection) {
      return ResourceUtils.adapt(SelectionUtils.getSingleElement(selection));
    }
    return null;
  }

  @Override
  protected void open(ISonarResource sonarResource) {
    SonarProject sonarProject = SonarProject.getInstance(sonarResource.getProject());
    String url = new SonarUrls().resourceUrl(sonarResource);
    ISonarServer sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (sonarServer == null) {
      showMessage(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getProject().getName(), url));
      return;
    }
    if (sonarServer.disabled()) {
      showMessage("Server is disabled.");
      return;
    }
    if (!WSClientFactory.getSonarClient(sonarServer).exists(sonarResource.getKey())) {
      showMessage("Not found.");
      return;
    }
    super.open(sonarProject, url);
  }

}
