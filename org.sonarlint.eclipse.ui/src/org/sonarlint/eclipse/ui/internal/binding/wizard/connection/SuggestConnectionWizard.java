/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.sonarlint.eclipse.core.SonarLintLogger;

/** Wizard that is containing only the token page for Connected Mode suggestions */
public class SuggestConnectionWizard extends AbstractConnectionWizard {
  public SuggestConnectionWizard(ServerConnectionModel model) {
    super("Connected Mode suggestion for SonarQube (Server, Cloud)", model);
  }

  @Override
  protected void actualHandlePageChanging(PageChangingEvent event) {
    // Nothing to do
  }

  @Override
  protected IWizardPage getActualStartingPage() {
    return tokenPage;
  }

  @Override
  protected void actualAddPages() {
    addPage(tokenPage);
  }

  @Override
  protected IWizardPage getActualNextPage(IWizardPage page) {
    return null;
  }

  @Override
  protected boolean actualCanFinish() {
    // INFO: testConnection(...) won't fail if model.getUsername(...) is null -.-
    return model.getUsername() != null && testConnection(model.getOrganization());
  }

  @Override
  protected boolean actualPerformFinish() {
    try {
      if (!testConnection(model.getOrganization())) {
        return false;
      }

      finalizeConnectionCreation();
      return true;
    } catch (Exception e) {
      var currentPage = (DialogPage) getContainer().getCurrentPage();
      currentPage.setErrorMessage("Cannot create connection: " + e.getMessage());
      SonarLintLogger.get().error("Error when finishing suggest connection wizard", e);
      return false;
    }
  }
}
