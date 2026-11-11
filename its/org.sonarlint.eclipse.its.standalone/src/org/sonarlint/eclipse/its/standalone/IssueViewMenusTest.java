/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarlint.eclipse.its.standalone;

import org.eclipse.reddeer.workbench.impl.view.AbstractView;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.RenderedViewMenuItem;
import org.sonarlint.eclipse.its.shared.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintTaintVulnerabilitiesView;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueViewMenusTest extends AbstractSonarLintTest {

  @Test
  public void issue_views_expose_marker_menu_contributions() {
    for (AbstractView view : new AbstractView[] {new OnTheFlyView(), new ReportView(), new SonarLintTaintVulnerabilitiesView()}) {
      view.open();
      view.activate();
      for (var label : new String[] {"Show", "Group By", "Sort By", "Filters...", "Configure Columns..."}) {
        assertThat(new RenderedViewMenuItem(label).getText().replace("&", "")).isEqualTo(label);
      }
      assertThat(new RenderedViewMenuItem("Group By", "None").isEnabled()).isTrue();
    }
  }
}
