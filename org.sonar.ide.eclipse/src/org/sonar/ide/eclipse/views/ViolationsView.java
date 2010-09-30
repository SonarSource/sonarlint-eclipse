/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.jobs.RefreshAllViolationsJob;

/**
 * @author Jérémie Lagarde
 */
public class ViolationsView extends MarkerSupportView {

  public static final String ID = "org.sonar.ide.eclipse.views.ViolationsView";

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
      refreshAction.setImageDescriptor(SonarImages.getImageDescriptor(SonarImages.IMG_SONARREFRESH));
    }
    return refreshAction;
  }

  @Override
  public String getContentDescription() {
    // TODO : add some metrics about violation makers.
    return "";
  }

}
