/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.bind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.RemoteSonarProject;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

/**
 * This provider will search for projects on all configured SonarQube servers
 * then return a list of proposals. Because a proposal can only be a String
 * we will serialize {@link RemoteSonarProject} using {@link RemoteSonarProject#asString()}
 *
 */
public class SearchEngineProvider implements IContentProposalProvider {

  private final WizardPage parentPage;
  private final IServer server;
  private TextSearchIndex<RemoteProject> projectIndex;

  public SearchEngineProvider(IServer server, WizardPage parentPage) {
    this.server = server;
    this.parentPage = parentPage;
  }

  public TextSearchIndex<RemoteProject> getProjectIndex() {
    if (projectIndex == null && server.isStorageUpdated()) {
      projectIndex = server.getProjectIndex();
    }
    return projectIndex;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    if (!server.isStorageUpdated()) {
      parentPage.setMessage("Please refresh project list from server first", IMessageProvider.INFORMATION);
      return new IContentProposal[0];
    }
    List<IContentProposal> list = new ArrayList<>();
    try {
      Map<RemoteProject, Double> modules = getProjectIndex().search(contents);
      List<Map.Entry<RemoteProject, Double>> entries = new ArrayList<>(modules.entrySet());
      entries.sort(
        Comparator.comparing(Map.Entry<RemoteProject, Double>::getValue).reversed()
          .thenComparing(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)));
      for (Map.Entry<RemoteProject, Double> e : entries) {
        RemoteProject project = e.getKey();
        RemoteSonarProject prj = new RemoteSonarProject(server.getId(), project.getKey(), project.getName());
        list.add(new ContentProposal(prj.asString(), e.getKey().getName(), prj.getDescription()));
      }
    } catch (Exception e) {
      SonarLintLogger.get().debug("Unable to search projects from server " + server.getId(), e);
    }
    if (!list.isEmpty()) {
      parentPage.setMessage("", IMessageProvider.NONE);
      return list.toArray(new IContentProposal[list.size()]);
    } else {
      parentPage.setMessage("No results", IMessageProvider.INFORMATION);
      return new IContentProposal[0];
    }
  }

}
