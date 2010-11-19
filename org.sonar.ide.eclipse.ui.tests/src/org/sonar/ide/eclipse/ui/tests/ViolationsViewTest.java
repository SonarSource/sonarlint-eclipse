/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.Test;
import org.sonar.ide.eclipse.internal.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.ui.views.ViolationsView;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;
import org.sonar.ide.eclipse.ui.tests.utils.SwtBotUtils;

public class ViolationsViewTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void shouldShowViolationsForSelectionAndChildren() throws Exception {
    SwtBotUtils.openPerspective(bot, ISonarConstants.PERSPECTIVE_ID);

    SWTBotView view = bot.viewById(ViolationsView.ID);
    view.show();

    assertThat(view.bot().tree().hasItems(), is(false));

    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();
    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);

    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java");

    bot.sleep(5000);

    assertThat(view.bot().tree().hasItems(), is(true));
  }
}
