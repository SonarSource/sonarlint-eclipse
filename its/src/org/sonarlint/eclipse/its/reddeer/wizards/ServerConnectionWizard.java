/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.wizards;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.RadioButton;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.swt.widgets.Table;
import org.sonarlint.eclipse.its.reddeer.conditions.OrganizationsAreFetched;

public class ServerConnectionWizard extends NewMenuWizard {
  public ServerConnectionWizard() {
    super("Connect to SonarQube or SonarCloud", "SonarLint", "New SonarQube/SonarCloud Connection");
  }

  public static class ServerTypePage extends WizardPage {

    public ServerTypePage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void selectSonarCloud() {
      new RadioButton(this).click();
    }

    public void selectSonarQube() {
      new RadioButton(this, 1).click();
    }
  }

  public static class ServerUrlPage extends WizardPage {

    public ServerUrlPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void setUrl(String url) {
      new DefaultText(this).setText(url);
    }
  }

  public static class AuthenticationModePage extends WizardPage {

    public AuthenticationModePage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void selectUsernamePasswordMode() {
      new RadioButton(this, 1).click();
    }
  }

  public static class AuthenticationPage extends WizardPage {

    public AuthenticationPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void setToken(String token) {
      new DefaultText(this).setText(token);
    }

    public void setUsername(String adminLogin) {
      new DefaultText(this).setText(adminLogin);
    }

    public void setPassword(String password) {
      new DefaultText(this, 1).setText(password);
    }
  }

  public static class OrganizationsPage extends WizardPage {

    public OrganizationsPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void waitForOrganizationsToBeFetched() {
      new WaitUntil(new OrganizationsAreFetched(this));
    }

    public String getOrganization() {
      return new DefaultText(this).getText();
    }

    public void typeOrganizationAndSelectFirst(String organizationName) {
      new DefaultText(this).setText(organizationName);

      new WaitUntil(new WidgetIsFound(Table.class));
      new DefaultTable(this).getItem(0).select();
    }

    public void setOrganization(String organizationName) {
      new DefaultText(this).setText(organizationName);
    }
  }

  public static class ConnectionNamePage extends WizardPage {

    public ConnectionNamePage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void setConnectionName(String connectionName) {
      new DefaultText(this).setText(connectionName);
    }

    public String getConnectionName() {
      return new DefaultText(this).getText();
    }
  }

  public static class NotificationsPage extends WizardPage {

    public NotificationsPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public boolean areNotificationsEnabled() {
      return new CheckBox(this).isChecked();
    }
  }

}
