/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.command;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.ui.internal.views.IssueEditorWebView;

public class EditIssueCommand extends AbstractIssueCommand {

  private static final Logger LOG = LoggerFactory.getLogger(EditIssueCommand.class);

  @Override
  protected void execute(IMarker selectedMarker) {
    try {
      IssueEditorWebView view = (IssueEditorWebView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueEditorWebView.ID);
      view.setInput(selectedMarker);
    } catch (Exception e) {
      LOG.error("Unable to open Issue Editor Web View", e);
    }
  }

}
