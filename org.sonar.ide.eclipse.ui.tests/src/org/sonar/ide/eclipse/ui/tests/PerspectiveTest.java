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

import org.eclipse.jdt.ui.JavaUI;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.ui.views.HotspotsView;
import org.sonar.ide.eclipse.internal.ui.views.MeasuresView;
import org.sonar.ide.eclipse.internal.ui.views.ViolationsView;
import org.sonar.ide.eclipse.internal.ui.views.WebView;
import org.sonar.ide.eclipse.ui.tests.utils.SwtBotUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PerspectiveTest extends UITestCase {

  @Test
  public void allViewsArePresent() {
    SwtBotUtils.openPerspective(bot, ISonarConstants.PERSPECTIVE_ID);

    bot.viewById(JavaUI.ID_PACKAGES);

    bot.viewById(WebView.ID);
    bot.viewById(HotspotsView.ID);
    bot.viewById(ViolationsView.ID);

    bot.viewById(MeasuresView.ID);

    assertThat(bot.views().size(), is(5));
  }

}
