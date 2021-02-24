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
package org.sonarlint.eclipse.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;

public class ProjectBindingWizardBot {
  private static final String NEXT_MNEMONIC = "Next >";

  private final SWTBot bot;

  public ProjectBindingWizardBot(SWTWorkbenchBot bot) {
    this.bot = bot.shell("Bind to a SonarQube or SonarCloud project").bot();
  }

  public ProjectBindingWizardBot clickAdd() {
    bot.button("Add...").click();
    return this;
  }

  public ProjectBindingWizardBot chooseProject(String projectName) {
    SWTBot dialogBot = bot.shell("Project selection").bot();
    dialogBot.text().setText(projectName);
    dialogBot.button("OK").click();
    return this;
  }

  public ProjectBindingWizardBot clickNext() {
    bot.button(NEXT_MNEMONIC).click();
    return this;
  }

  public void clickFinish() {
    bot.button("Finish").click();
  }

  public ProjectBindingWizardBot waitForOrganizationProjectsToBeFetched() {
    bot.waitUntil(new DefaultCondition() {

      @Override
      public boolean test() {
        return bot.text().isEnabled();
      }

      @Override
      public String getFailureMessage() {
        return "Cannot fetch organization's projects";
      }
    }, 20_000);
    return this;
  }

  public String getProjectKey() {
    return bot.text().getText();
  }

  public ProjectBindingWizardBot typeProjectKey(String projectKey) {
    bot.text()
      .setText("")
      .setText(projectKey);
    return this;
  }

}
