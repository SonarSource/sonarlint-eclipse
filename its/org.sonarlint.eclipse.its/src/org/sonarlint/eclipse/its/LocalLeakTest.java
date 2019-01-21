/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.OnTheFlyViewBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class LocalLeakTest extends AbstractSonarLintTest {

  private static final String CREATIONDATE_ATT = "creationdate";

  @Test
  public void shouldComputeLocalLeak() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);

    SWTBotView view = new OnTheFlyViewBot(bot).show();
    assertThat(view.bot().tree().columns()).containsExactly("Date", "Description", "Resource");
    assertThat(view.bot().tree().getAllItems()).isEmpty();

    IProject project = importEclipseProject("java/leak", "leak");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("leak", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, CREATIONDATE_ATT)).containsOnly(
      tuple("/leak/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger.", null));

    // TODO We could maybe force view to refresh without having to select again the resource
    new JavaPackageExplorerBot(bot)
      .expandAndSelect("leak", "src", "hello", "Hello.java");

    assertThat(view.bot().tree().getAllItems()).hasSize(1);
    assertThat(view.bot().tree().cell(0, 0)).isEqualTo("");

    // Change content
    SWTBotEclipseEditor editor = bot.editorByTitle("Hello.java").toTextEditor();
    editor.navigateTo(7, 43);
    editor.insertText("\nSystem.out.println(\"Hello1\");");
    editor.save();

    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/leak/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/leak/src/hello/Hello.java", 10, "Replace this use of System.out or System.err by a logger."));

    // TODO We could maybe force view to refresh without having to select again the resource
    new JavaPackageExplorerBot(bot)
      .expandAndSelect("leak", "src", "hello", "Hello.java");

    assertThat(view.bot().tree().getAllItems()).hasSize(2);
    assertThat(view.bot().tree().cell(0, 0)).isEqualTo("few seconds ago");
    assertThat(view.bot().tree().cell(1, 0)).isEqualTo("");
  }

  @Test
  public void dontLooseLeakOnParsingError() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);

    IProject project = importEclipseProject("js/js-simple", "js-simple");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("js-simple", "src", "hello.js");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello.js").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, CREATIONDATE_ATT)).containsOnly(
      tuple("/js-simple/src/hello.js", 2, "Remove this usage of alert(...).", null));
    long markerId = markers.get(0).getId();

    // Change content
    SWTBotEclipseEditor editor = bot.editorByTitle("hello.js").toTextEditor();
    editor.navigateTo(1, 23);
    editor.insertText("\nvar i;");
    editor.save();

    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello.js").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/js-simple/src/hello.js", 2, "Remove this usage of alert(...)."),
      tuple("/js-simple/src/hello.js", 3, "Remove the declaration of the unused 'i' variable."));

    IMarker newIssue = markers.stream().filter(m -> m.getId() != markerId).findFirst().get();
    String timestamp = (String) newIssue.getAttribute(CREATIONDATE_ATT);
    assertThat(timestamp).isNotEmpty();

    // Insert content that should crash analyzer
    editor.navigateTo(2, 8);
    editor.insertText("\nvar");
    editor.save();
    JobHelpers.waitForJobsToComplete(bot);

    // Issues are still there
    markers = Arrays.asList(project.findMember("src/hello.js").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, CREATIONDATE_ATT)).containsOnly(
      tuple("/js-simple/src/hello.js", 2, "Remove this usage of alert(...).", null),
      tuple("/js-simple/src/hello.js", 3, "Remove the declaration of the unused 'i' variable.", timestamp));

  }

}
