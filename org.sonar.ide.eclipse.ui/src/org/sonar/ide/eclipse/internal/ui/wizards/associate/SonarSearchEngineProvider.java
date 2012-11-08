/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.ui.wizards.associate;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * This provider will search for projects on all configured Sonar servers
 * then return a list of proposals. Because a proposal can only be a String
 * we will serialize {@link RemoteSonarProject} using {@link RemoteSonarProject#asString()}
 * @author julien
 *
 */
public class SonarSearchEngineProvider implements IContentProposalProvider {

  private final List<Host> hosts;

  public SonarSearchEngineProvider(List<Host> hosts) {
    this.hosts = hosts;
  }

  public IContentProposal[] getProposals(String contents, int position) {
    ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
    for (Host host : hosts) {
      String url = host.getHost();
      ResourceQuery query = new ResourceQuery().setScopes(Resource.SCOPE_SET).setQualifiers(Resource.QUALIFIER_PROJECT,
          Resource.QUALIFIER_MODULE);
      Sonar sonar = SonarCorePlugin.getServersManager().getSonar(url);
      List<Resource> resources = sonar.findAll(query);
      for (Resource resource : resources) {
        if (resource.getName().toLowerCase().contains(contents.toLowerCase())) {
          RemoteSonarProject prj = new RemoteSonarProject(host.getHost(), resource.getKey(), resource.getName());
          list.add(new ContentProposal(prj.asString(), resource.getName(), prj.getDescription()));
        }
      }
    }
    return (IContentProposal[]) list.toArray(new IContentProposal[list.size()]);
  }

}
