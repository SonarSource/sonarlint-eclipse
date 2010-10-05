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

package org.sonar.ide.eclipse.tests.common;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author Evgeny Mandrikov
 */
public class JobHelpers {

  /**
   * Inspired by http://eclipse.dzone.com/articles/eclipse-gui-testing-is-viable-
   * Also see http://fisheye.jboss.org/browse/JBossTools/trunk/vpe/tests/org.jboss.tools.vpe.ui.test/src/org/jboss/tools/vpe/ui/test/TestUtil.java?r=HEAD
   */
  public static void waitForJobsToComplete() {
    // ensure that all queued workspace operations and locks are released
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

        public void run(IProgressMonitor monitor) throws CoreException {
          // nothing to do!
        }
      }, new NullProgressMonitor());
    } catch (CoreException e) {
      throw new IllegalStateException(e);
    }

  }

  private JobHelpers() {
  }
}
