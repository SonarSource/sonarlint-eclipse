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
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.ide.eclipse.wsclient.ISonarRemoteModule;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * This provider will search for projects on all configured Sonar servers
 * then return a list of proposals. Because a proposal can only be a String
 * we will serialize {@link RemoteSonarProject} using {@link RemoteSonarProject#asString()}
 *
 */
public class SonarSearchEngineProvider implements IContentProposalProvider {

  private final Collection<SonarServer> sonarServers;
  private final WizardPage parentPage;

  public SonarSearchEngineProvider(Collection<SonarServer> sonarServers, WizardPage parentPage) {
    this.sonarServers = sonarServers;
    this.parentPage = parentPage;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    List<IContentProposal> list = new ArrayList<IContentProposal>();
    for (SonarServer sonarServer : sonarServers) {
      if (sonarServer.disabled()) {
        continue;
      }
      List<ISonarRemoteModule> remoteModules = WSClientFactory.getSonarClient(sonarServer).searchRemoteModules(contents);
      for (ISonarRemoteModule resource : remoteModules) {
        RemoteSonarProject prj = new RemoteSonarProject(sonarServer.getUrl(), resource.getKey(), resource.getName());
        list.add(new ContentProposal(prj.asString(), resource.getName(), prj.getDescription()));
      }
    }
    if (!list.isEmpty()) {
      parentPage.setMessage("", IMessageProvider.NONE);
      return list.toArray(new IContentProposal[list.size()]);
    } else {
      parentPage.setMessage("No result", IMessageProvider.INFORMATION);
      return new IContentProposal[0];
    }
  }

}
