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

package org.sonar.ide.eclipse.internal.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.jobs.RefreshAllViolationsJob;

/**
 * @author Jérémie Lagarde
 */
public class ViolationsView extends MarkerSupportView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.ViolationsView";

  private Action refreshAction;

  public ViolationsView() {
    super("org.sonar.ide.eclipse.markers.violationMarkerGenerator");
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    createToolbar();
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(getRefreshAction());
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private Action getRefreshAction() {
    if (refreshAction == null) {
      refreshAction = new Action() {
        @Override
        public void run() {
          RefreshAllViolationsJob.createAndSchedule();
        }
      };
      refreshAction.setText(Messages.getString("action.refresh.violations")); //$NON-NLS-1$
      refreshAction.setToolTipText(Messages.getString("action.refresh.violations.desc")); //$NON-NLS-1$
      refreshAction.setImageDescriptor(SonarImages.SONARREFRESH_IMG);
    }
    return refreshAction;
  }

  /**
   * TODO quote from {@link WorkbenchPart#getContentDescription()} : "It is considered bad practise to overload or extend this method."
   */
  @Override
  public String getContentDescription() {
    // TODO : add some metrics about violation makers.
    return "";
  }

}
