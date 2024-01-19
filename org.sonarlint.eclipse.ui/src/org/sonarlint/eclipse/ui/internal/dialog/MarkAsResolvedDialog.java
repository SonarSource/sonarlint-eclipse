/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ResolutionStatus;

/** Dialog for marking an issue as resolved using the possible transitions */
public class MarkAsResolvedDialog extends Dialog {
  private final ArrayList<IssueStatusRadioButton> issueStatusRadioButtons = new ArrayList<>();
  private Text commentSection;
  private final List<ResolutionStatus> transitions;

  private final String formattingHelpURL;
  private ResolutionStatus finalTransition;
  @Nullable
  private String finalComment;
  private final boolean isSonarCloud;

  public MarkAsResolvedDialog(Shell parentShell, List<ResolutionStatus> transitions, String hostURL,
    boolean isSonarCloud) {
    super(parentShell);
    this.transitions = transitions;
    this.isSonarCloud = isSonarCloud;
    this.formattingHelpURL = hostURL + (isSonarCloud ? "/markdown/help" : "/formatting/help");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    return createDialogAreaInternally((Composite) super.createDialogArea(parent));
  }
  
  protected Composite createDialogAreaInternally(Composite container) {
    var group = new Group(container, SWT.NONE);
    group.setLayout(new GridLayout(1, true));
    var gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    group.setLayoutData(gridData);

    transitions.forEach(transition -> {
      var btn = new IssueStatusRadioButton(group, transition);
      btn.getButton().setText(transition.getTitle() + " - " + transition.getDescription());

      var innerGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      innerGridData.grabExcessHorizontalSpace = true;
      btn.getButton().setLayoutData(innerGridData);

      issueStatusRadioButtons.add(btn);
    });
    issueStatusRadioButtons.get(0).getButton().setSelection(true);

    var commentTitle = new Label(container, SWT.NONE);
    gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    commentTitle.setLayoutData(gridData);
    commentTitle.setText("Add a comment (optional)");

    commentSection = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    commentSection.setLayoutData(gridData);

    var commentHelp = new Link(container, SWT.NONE);
    gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    commentHelp.setLayoutData(gridData);
    commentHelp.setText("<a>Formatting Help</a>: *Bold* `Code` * Bulleted point");
    commentHelp.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtils.openExternalBrowser(formattingHelpURL, MarkAsResolvedDialog.this.getShell().getDisplay());
      }
    });

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Mark Issue as Resolved", true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText("Mark Issue as Resolved on " + (isSonarCloud ? "SonarCloud" : "SonarQube"));
    newShell.setMinimumSize(500, 300);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  @Override
  protected void okPressed() {
    // INFO: At every point in time there is exactly one radio button selected!
    this.finalTransition = issueStatusRadioButtons.stream()
      .filter(it -> it.getButton().getSelection())
      .findFirst()
      .get().getIssueStatus();
    this.finalComment = commentSection.getText();

    super.okPressed();
  }

  /** After finishing the dialog we can use this for invoking SQ / SC API */
  public ResolutionStatus getFinalTransition() {
    return this.finalTransition;
  }

  /** After finishing the dialog we can use this for invoking SQ / SC API */
  public String getFinalComment() {
    return this.finalComment;
  }

  /** Utility class to wrap a SWT Radio Button with its corresponding IssueStatus */
  static class IssueStatusRadioButton {
    private final Button radioButton;
    private final ResolutionStatus issueStatus;

    public IssueStatusRadioButton(Composite parent, ResolutionStatus issueStatus) {
      this.issueStatus = issueStatus;
      this.radioButton = new Button(parent, SWT.RADIO);
    }

    public Button getButton() {
      return radioButton;
    }

    public ResolutionStatus getIssueStatus() {
      return issueStatus;
    }
  }
}
