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
package org.sonarlint.eclipse.its;

import java.util.Arrays;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MavenTest extends AbstractSonarLintTest {

  @Test
  public void shouldNotAnalyzeResourcesInSubModules() throws Exception {
    new JavaPerspective().open();
    IProject root = importEclipseProject("java/maven", "sample-maven");
    IProject module1 = importEclipseProject("java/maven/sample-module1", "sample-module1");
    importEclipseProject("java/maven/sample-module2", "sample-module2");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("sample-maven", "sample-module1", "src", "main", "java", "hello", "Hello1.java");
    JobHelpers.waitForJobsToComplete(bot);

    assertThat(Arrays.asList(root.findMember("sample-module1/src/main/java/hello/Hello1.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE))).isEmpty();
    assertThat(Arrays.asList(root.findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE))).isEmpty();

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("sample-module1", "src/main/java", "hello", "Hello1.java");
    JobHelpers.waitForJobsToComplete(bot);

    assertThat(Arrays.asList(root.findMember("sample-module1/src/main/java/hello/Hello1.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE))).isEmpty();
    assertThat(Arrays.asList(root.findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE))).isEmpty();

    assertThat(Arrays.asList(module1.findMember("src/main/java/hello/Hello1.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE)))
      .extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
        tuple("/sample-module1/src/main/java/hello/Hello1.java", 9, "Replace this use of System.out or System.err by a logger."));

    if (!platformVersion().toString().startsWith("4.4") && !platformVersion().toString().startsWith("4.5")) {
      // Issues on pom.xml
      new JavaPackageExplorerBot(bot)
        .expandAndDoubleClick("sample-maven", "pom.xml");
      JobHelpers.waitForJobsToComplete(bot);
      assertThat(Arrays.asList(root.findMember("pom.xml").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE)))
        .extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
          tuple("/sample-maven/pom.xml", 11, "Replace \"pom.name\" with \"project.name\"."));
    }
  }

}
