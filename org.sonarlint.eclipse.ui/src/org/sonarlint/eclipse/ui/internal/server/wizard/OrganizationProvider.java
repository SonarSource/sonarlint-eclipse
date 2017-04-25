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
import org.sonarsource.sonarlint.core.client.api.exceptions.UnsupportedServerException;
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
        parentPage.getWizard().getContainer().run(false, false, new IRunnableWithProgress() {

          @Override
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            orgs = transcientServer.getOrganizationsIndex(parentPage.getUsername(), parentPage.getPassword());
          }
        });
        parentPage.setMessage("", IMessageProvider.NONE);
        return filtered(contents);
      } catch (UnsupportedServerException e) {
        parentPage.setMessage("No organizations on this server", IMessageProvider.INFORMATION);
        return new IContentProposal[0];
      } catch (Exception e) {
        SonarLintLogger.get().debug("Unable to search organizations on server " + transcientServer.getHost(), e);
        parentPage.setMessage(e.getMessage(), IMessageProvider.ERROR);
        return new IContentProposal[0];
      }
    } else {
      return filtered(contents);
    }

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
    for (RemoteOrganization o : filtered.keySet()) {
      list.add(new ContentProposal(o.getKey(), o.getName(), o.getDescription()));
    }
    return list.toArray(new IContentProposal[list.size()]);
  }

}
