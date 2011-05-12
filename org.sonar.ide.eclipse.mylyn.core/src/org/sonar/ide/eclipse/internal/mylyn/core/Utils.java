/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;

public final class Utils {

  public static PriorityLevel toMylynPriority(String value) {
    if ("BLOCKER".equals(value)) { //$NON-NLS-1$
      return PriorityLevel.P1;
    } else if ("CRITICAL".equals(value)) { //$NON-NLS-1$
      return PriorityLevel.P2;
    } else if ("MAJOR".equals(value)) { //$NON-NLS-1$
      return PriorityLevel.P3;
    } else if ("MINOR".equals(value)) { //$NON-NLS-1$
      return PriorityLevel.P4;
    } else if ("INFO".equals(value)) { //$NON-NLS-1$
      return PriorityLevel.P5;
    } else {
      return null;
    }
  }

  private Utils() {
  }

}
