/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ResolutionStatus;

/** Dialog for marking anticipated an issue as resolved using the possible transitions */
public class MarkAnticipatedIssueAsResolvedDialog extends MarkAsResolvedDialog {
  public MarkAnticipatedIssueAsResolvedDialog(Shell parentShell,
    List<ResolutionStatus> transitions, String hostURL) {
    super(parentShell, transitions, hostURL, false);
  }

  @Override
  protected Composite createDialogAreaInternally(Composite container) {
    var adjustedContainer = super.createDialogAreaInternally(container);

    // Add disclaimer for what is an anticipated issue and how it differs from the server issue key
    var commentHelp = new Link(adjustedContainer, SWT.NONE);
    commentHelp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    commentHelp.setText("This is for a local Issue expected to appear on SonarQube after the next analysis. "
      + "<a>Learn more</a>");
    commentHelp.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtils.openExternalBrowser(SonarLintDocumentation.MARK_ISSUES_LINK, getShell().getDisplay());
      }
    });

    return adjustedContainer;
  }
}
