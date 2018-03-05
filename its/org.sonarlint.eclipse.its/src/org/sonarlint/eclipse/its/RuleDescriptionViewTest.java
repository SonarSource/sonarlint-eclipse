/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarlint.eclipse.its;

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.OnTheFlyViewBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RuleDescriptionViewTest extends AbstractSonarLintTest {

  @Test
  public void openRuleDescription() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);

    SWTBotView view = new OnTheFlyViewBot(bot).show();

    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));

    // TODO We could maybe force view to refresh without having to select again the resource
    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-simple", "src", "hello", "Hello.java");

    view.bot().tree().select(0).contextMenu("Rule description").click();

    SWTBotView descView = bot.viewById("org.sonarlint.eclipse.ui.views.RuleDescriptionWebView");
    assertThat(descView.isActive()).isTrue();
    descView.show();

    if (platformVersion().compareTo(new Version("4.4")) >= 0) {
      Browser b = (Browser) bot.getFinder().findControls(descView.getWidget(), CoreMatchers.instanceOf(Browser.class), true).get(0);

      String text = UIThreadRunnable.syncExec(bot.getDisplay(), (Result<String>) b::getText);
      assertThat(text).contains("squid:S106",
        "Sensitive data must only be logged securely", "CERT, ERR02-J");
    }
  }

}
