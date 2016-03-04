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
package org.sonarlint.eclipse.ui.internal.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;

/**
 * This provider will search for projects on all configured SonarQube servers
 * then return a list of proposals. Because a proposal can only be a String
 * we will serialize {@link RemoteSonarProject} using {@link RemoteSonarProject#asString()}
 *
 */
public class SonarSearchEngineProvider implements IContentProposalProvider {

  private final Collection<IServer> sonarServers;
  private final WizardPage parentPage;

  public SonarSearchEngineProvider(Collection<IServer> sonarServers, WizardPage parentPage) {
    this.sonarServers = sonarServers;
    this.parentPage = parentPage;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    List<IContentProposal> list = new ArrayList<>();
    for (IServer sonarServer : sonarServers) {
      try {
        List<RemoteModule> modules = sonarServer.findModules(contents);
        for (RemoteModule m : modules) {
          RemoteSonarProject prj = new RemoteSonarProject(sonarServer.getId(), m.getKey(), m.getName());
          list.add(new ContentProposal(prj.asString(), m.getName(), prj.getDescription()));
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().debug("Unable to search modules from server " + sonarServer.getName(), e);
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
