/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.sonarsource.sonarlint.core.serverapi.organization.ServerOrganization;

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
      var allUserOrgs = model.getUserOrgs();
      if (allUserOrgs != null) {
        allUserOrgs.stream().limit(10).forEach(o -> list.add(new ContentProposal(o.getKey(), o.getName(), toDescription(o))));
      }
    } else {
      var organizationsIndex = model.getUserOrgsIndex();
      Map<ServerOrganization, Double> filtered = organizationsIndex != null ? organizationsIndex.search(contents) : Collections.emptyMap();
      if (filtered.isEmpty()) {
        parentPage.setMessage("No results", IMessageProvider.INFORMATION);
      } else {
        parentPage.setMessage("", IMessageProvider.NONE);
      }
      filtered.entrySet()
        .stream()
        .sorted(Comparator.comparing(Map.Entry<ServerOrganization, Double>::getValue).reversed()
          .thenComparing(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)))
        .map(Map.Entry::getKey)
        .forEach(o -> list.add(new ContentProposal(o.getKey(), o.getName(), toDescription(o))));
    }
    return list.toArray(new IContentProposal[list.size()]);
  }

  private static String toDescription(ServerOrganization org) {
    var sb = new StringBuilder();
    sb.append("Name: ").append(org.getName()).append("\n");
    sb.append("Key: ").append(org.getKey()).append("\n");
    sb.append("Description: ").append(org.getDescription());
    return sb.toString();
  }

}
