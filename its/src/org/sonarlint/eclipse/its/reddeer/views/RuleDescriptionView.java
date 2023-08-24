/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.views;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.swt.api.Browser;
import org.eclipse.reddeer.swt.api.Label;
import org.eclipse.reddeer.swt.api.TabFolder;
import org.eclipse.reddeer.swt.condition.PageIsLoaded;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.tab.DefaultTabFolder;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;

public class RuleDescriptionView extends WorkbenchView {
  public RuleDescriptionView() {
    super("SonarLint Rule Description");
  }

  public Label getRuleName() {
    return new DefaultLabel(getCTabItem(), 0);
  }
  
  public Label getCleanCodeAttributeCategory() {
    return new DefaultLabel(getCTabItem(), 1);
  }
  
  public Label getFirstSoftwareQuality() {
    return new DefaultLabel(getCTabItem(), 2);
  }
  
  public Label getSecondSoftwareQuality() {
    return new DefaultLabel(getCTabItem(), 4);
  }
  
  public Label getThirdSoftwareQuality() {
    return new DefaultLabel(getCTabItem(), 6);
  }

  public Label getRuleKey() {
    return new DefaultLabel(getCTabItem(), 8);
  }

  public Browser getFirstBrowser() {
    // Browser can take a while to render
    new WaitUntil(new WidgetIsFound(org.eclipse.swt.browser.Browser.class, getCTabItem().getControl()),
      TimePeriod.DEFAULT, false);
    return new InternalBrowser(getCTabItem());
  }

  public TabFolder getSections() {
    return new DefaultTabFolder(getCTabItem());
  }

  public String getFlatTextContent() {
    new WaitUntil(new PageIsLoaded(getFirstBrowser()));

    return getRuleName().getText() + "\n"
      + getCleanCodeAttributeCategory().getText() + " "
      + getFirstSoftwareQuality().getText() + " "
      + getSecondSoftwareQuality().getText() + " "
      + getThirdSoftwareQuality().getText() + " "
      + getRuleKey().getText() + "\n"
      + getFirstBrowser().getText();
  }
}
