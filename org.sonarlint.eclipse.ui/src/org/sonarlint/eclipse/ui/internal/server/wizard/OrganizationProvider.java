/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class OrganizationProvider implements IContentProposalProvider {

  private final ServerLocationWizardPage parentPage;
  private String previousHost;
  private String previousUsername;
  private String previousPassword;
  private TextSearchIndex<RemoteOrganization> orgs;

  public OrganizationProvider(ServerLocationWizardPage parentPage) {
    this.parentPage = parentPage;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    IServer transcientServer = parentPage.transcientServer();

    if (!Objects.equals(previousHost, transcientServer.getHost())
      || !Objects.equals(previousUsername, parentPage.getUsername())
      || !Objects.equals(previousPassword, parentPage.getPassword())) {

      previousHost = transcientServer.getHost();
      previousUsername = parentPage.getUsername();
      previousPassword = parentPage.getPassword();
      try {
        parentPage.getWizard().getContainer().run(true, true, new IRunnableWithProgress() {

          @Override
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            orgs = transcientServer.getOrganizationsIndex(previousUsername, previousPassword, monitor);
            parentPage.setMessage("", IMessageProvider.NONE);
          }
        });
      } catch (InvocationTargetException e) {
        SonarLintLogger.get().debug("Unable to download organizations", e.getCause());
        orgs = null;
        parentPage.setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      } catch (InterruptedException e) {
        orgs = null;
        parentPage.setMessage("", IMessageProvider.NONE);
      }
    }
    return filtered(contents);

  }

  private IContentProposal[] filtered(String contents) {
    if (orgs == null) {
      // Keep previous message
      return new IContentProposal[0];
    }
    if (orgs.isEmpty()) {
      parentPage.setMessage("No organizations on this server", IMessageProvider.INFORMATION);
      return new IContentProposal[0];
    }
    List<IContentProposal> list = new ArrayList<>();
    Map<RemoteOrganization, Double> filtered = orgs.search(contents);
    if (filtered.isEmpty()) {
      parentPage.setMessage("No results", IMessageProvider.INFORMATION);
    } else {
      parentPage.setMessage("", IMessageProvider.NONE);
    }
    List<Map.Entry<RemoteOrganization, Double>> entries = new ArrayList<>(filtered.entrySet());
    entries.sort(
      Comparator.comparing(Map.Entry<RemoteOrganization, Double>::getValue).reversed()
        .thenComparing(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)));
    for (Map.Entry<RemoteOrganization, Double> e : entries) {
      list.add(new ContentProposal(e.getKey().getKey(), e.getKey().getName(), toDescription(e.getKey())));
    }
    return list.toArray(new IContentProposal[list.size()]);
  }

  private static String toDescription(RemoteOrganization org) {
    StringBuilder sb = new StringBuilder();
    sb.append("Name: ").append(org.getName()).append("\n");
    sb.append("Key: ").append(org.getKey()).append("\n");
    sb.append("Description: ").append(org.getDescription());
    return sb.toString();
  }

}
