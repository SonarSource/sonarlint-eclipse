/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.util.ArrayList;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.org.FuzzySearchUserOrganizationsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.org.ListUserOrganizationsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.org.OrganizationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TokenDto;

public class OrganizationProvider implements IContentProposalProvider {

  private final ServerConnectionModel model;
  private final WizardPage parentPage;

  public OrganizationProvider(ServerConnectionModel model, WizardPage parentPage) {
    this.model = model;
    this.parentPage = parentPage;
  }

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    var list = new ArrayList<IContentProposal>();
    if (contents.isEmpty()) {
      var allUserOrgs = SonarLintBackendService.get().getBackend().getConnectionService()
        .listUserOrganizations(new ListUserOrganizationsParams(Either.forLeft(new TokenDto(model.getUsername())), model.getProtocolRegionOrElseEU()))
        .join();
      allUserOrgs.getUserOrganizations().stream().limit(10).forEach(o -> list.add(new ContentProposal(o.getKey(), o.getName(), toDescription(o))));
    } else {
      var filtered = SonarLintBackendService.get().getBackend().getConnectionService()
        .fuzzySearchUserOrganizations(new FuzzySearchUserOrganizationsParams(Either.forLeft(new TokenDto(model.getUsername())), contents, model.getProtocolRegionOrElseEU()))
        .join();
      if (filtered.getTopResults().isEmpty()) {
        parentPage.setMessage("No results", IMessageProvider.INFORMATION);
      } else {
        parentPage.setMessage("", IMessageProvider.NONE);
      }
      filtered.getTopResults()
        .stream()
        .forEach(o -> list.add(new ContentProposal(o.getKey(), o.getName(), toDescription(o))));
    }
    return list.toArray(new IContentProposal[list.size()]);
  }

  private static String toDescription(OrganizationDto org) {
    var sb = new StringBuilder();
    sb.append("Name: ").append(org.getName()).append("\n");
    sb.append("Key: ").append(org.getKey()).append("\n");
    sb.append("Description: ").append(org.getDescription());
    return sb.toString();
  }

}
