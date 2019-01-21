/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.bind.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class RemoteProjectProvider implements IContentProposalProvider {

  private final ProjectBindingModel model;
  private final WizardPage parentPage;

  public RemoteProjectProvider(ProjectBindingModel model, WizardPage parentPage) {
    this.model = model;
    this.parentPage = parentPage;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    List<IContentProposal> list = new ArrayList<>();
    TextSearchIndex<RemoteProject> projectIndex = model.getProjectIndex();
    Map<RemoteProject, Double> filtered = projectIndex != null ? projectIndex.search(contents) : Collections.emptyMap();
    if (filtered.isEmpty()) {
      parentPage.setMessage("No results", IMessageProvider.INFORMATION);
    } else {
      parentPage.setMessage("", IMessageProvider.NONE);
    }
    List<Map.Entry<RemoteProject, Double>> entries = new ArrayList<>(filtered.entrySet());
    entries.sort(
      Comparator.comparing(Map.Entry<RemoteProject, Double>::getValue).reversed()
        .thenComparing(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)));
    for (Map.Entry<RemoteProject, Double> e : entries) {
      list.add(new ProjectContentProposal(e.getKey()));
    }
    return list.toArray(new IContentProposal[list.size()]);
  }

  public static class ProjectContentProposal implements IContentProposal {

    private RemoteProject remoteProject;

    public ProjectContentProposal(RemoteProject remoteProject) {
      this.remoteProject = remoteProject;
    }

    @Override
    public String getContent() {
      return remoteProject.getKey();
    }

    @Override
    public int getCursorPosition() {
      return remoteProject.getKey().length();
    }

    @Override
    public String getLabel() {
      return remoteProject.getName();
    }

    public RemoteProject getRemoteProject() {
      return remoteProject;
    }

    @Override
    public String getDescription() {
      return new StringBuilder()
        .append("Name: ").append(remoteProject.getName()).append("\n")
        .append("Key: ").append(remoteProject.getKey()).append("\n")
        .toString();
    }

  }

}
