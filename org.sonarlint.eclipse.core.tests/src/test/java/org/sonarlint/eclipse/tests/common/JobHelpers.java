/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.tests.common;

import org.eclipse.core.runtime.jobs.Job;

/**
 * @author Evgeny Mandrikov
 */
public final class JobHelpers {

  /**
   * Inspired by http://fisheye.jboss.org/browse/JBossTools/trunk/vpe/tests/org.jboss.tools.vpe.ui.test/src/org/jboss/tools/vpe/ui/test/TestUtil.java?r=HEAD
   */
  public static void waitForJobsToComplete() {
    waitForIdle();
  }

  private static final long MAX_IDLE = 20 * 60 * 1000L;

  private static void waitForIdle() {
    waitForIdle(MAX_IDLE);
  }

  private static void waitForIdle(long maxIdle) {
    final long start = System.currentTimeMillis();
    while (!Job.getJobManager().isIdle()) {
      delay(500);
      if ((System.currentTimeMillis() - start) > maxIdle) {
        Job[] jobs = Job.getJobManager().find(null);
        StringBuffer jobsList = new StringBuffer("A long running job detected\n");
        for (Job job : jobs) {
          jobsList.append("\t").append(job.getName()).append("\n");
        }
        throw new RuntimeException(jobsList.toString());
      }
    }
  }

  private static void delay(long waitTimeMillis) {
    try {
      Thread.sleep(waitTimeMillis);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  private JobHelpers() {
  }
}
