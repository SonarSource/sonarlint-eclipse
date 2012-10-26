/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.jobs;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.EmbeddedSonarPlugin;
import org.sonar.batch.ProfileLoader;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.tests.common.JobHelpers;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AnalyseProjectJobTest extends SonarTestCase {

  @Before
  public void setUp() {
    EmbeddedSonarPlugin.getDefault().getSonarCustomizer().replace(ProfileLoader.class, FakeProfileLoader.class);
  }

  @After
  public void tearDown() {
    EmbeddedSonarPlugin.getDefault().getSonarCustomizer().restore(ProfileLoader.class);
  }

  @Test
  public void shouldAnalyse() throws Exception {
    IProject project = importEclipseProject("reference");

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    AnalyseProjectJob job = new AnalyseProjectJob(project);
    job.schedule();
    JobHelpers.waitForJobsToComplete();

    IMarker[] markers = project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers.length, is(3));

    assertThat(markers, hasItemInArray(new IsMarker("src/Findbugs.java", 5)));
    assertThat(markers, hasItemInArray(new IsMarker("src/Pmd.java", 2)));
    assertThat(markers, hasItemInArray(new IsMarker("src/Checkstyle.java", 1)));
  }

  static class IsMarker extends BaseMatcher<IMarker> {

    private final String path;
    private final int line;

    public IsMarker(String path, int line) {
      this.path = path;
      this.line = line;
    }

    public boolean matches(Object item) {
      IMarker marker = (IMarker) item;
      String actualPath = marker.getResource().getProjectRelativePath().toString();
      int actualLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
      return StringUtils.equals(actualPath, path) && (actualLine == line);
    }

    public void describeTo(Description description) {
      // TODO Auto-generated method stub
    }

  }

}
