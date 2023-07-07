/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.IssueStatus;

/** Dialog for marking an issue as resolved using the possible transitions */
public class MarkAsResolvedDialog extends Dialog {
  final ArrayList<IssueStatusCheckBox> issueStatusCheckBoxButtons = new ArrayList<>();
  private Text commentSection;
  private List<IssueStatus> transitions;
  
  private IssueStatus finalTransition;
  @Nullable
  private String finalComment;

  public MarkAsResolvedDialog(Shell parentShell, List<IssueStatus> transitions) {
    super(parentShell);
    this.transitions = transitions;
  }
  
  @Override
  protected Control createDialogArea(Composite parent) {
    var container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(2, true));
    
    transitions.forEach(transition -> {
      var checkBox = new IssueStatusCheckBox(container, transition);

      checkBox.getCheckBox().setText(transition.getTitle() + "\n" + transition.getDescription());
      var gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      gridData.grabExcessHorizontalSpace = true;
      gridData.horizontalSpan = 2;
      checkBox.getCheckBox().setLayoutData(gridData);
      
      // listener prohibiting de-selecting check boxes and multi-selection
      checkBox.getCheckBox().addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          checkBox.getCheckBox().setSelection(true);
          issueStatusCheckBoxButtons.stream()
            .filter(it -> it.getCheckBox() != checkBox.getCheckBox())
            .forEach(it -> it.getCheckBox().setSelection(false));
        }
      });
      
      issueStatusCheckBoxButtons.add(checkBox);
    });
    issueStatusCheckBoxButtons.get(0).getCheckBox().setSelection(true);
    
    var commentTitle = new Label(container, SWT.NONE);
    var gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalSpan = 2;
    commentTitle.setLayoutData(gridData);
    commentTitle.setText("\nAdd a comment (optional)");
    
    commentSection = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalSpan = 2;
    commentSection.setLayoutData(gridData);
    
    var commentHelp = new Link(container, SWT.NONE);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalSpan = 2;
    commentHelp.setLayoutData(gridData);
    // TODO: Where to get this link from?
    commentHelp.setText("<a href=\"https://next.sonarqube.com\">Formatting Help</a>: *Bold* `Code` * Bulleted point");
    
    return container;
  }
  
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Mark as resolved", true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }
  
  @Override
  protected Point getInitialSize() {
    return new Point(600, 300);
  }
  
  @Override
  protected void okPressed() {
    // INFO: At every point in time there is exactly one check box selected!
    this.finalTransition = issueStatusCheckBoxButtons.stream()
      .filter(it -> it.getCheckBox().getSelection())
      .findFirst()
      .get().getIssueStatus();
    this.finalComment = commentSection.getText();
    
    super.okPressed();
  }
  
  /** After finishing the dialog we can use this for invoking SQ / SC API */
  public IssueStatus getFinalTransition() {
    return this.finalTransition;
  }
  
  /** After finishing the dialog we can use this for invoking SQ / SC API */
  public String getFinalComment() {
    return this.finalComment;
  }
  
  /** Utility class to wrap a SWT Button (CheckBox) with its corresponding IssueStatus */
  static class IssueStatusCheckBox {
    private final Button checkBox;
    private final IssueStatus issueStatus;
    
    public IssueStatusCheckBox(Composite parent, IssueStatus issueStatus) {
      this.issueStatus = issueStatus;
      this.checkBox = new Button(parent, SWT.CHECK | SWT.BORDER);
    }
    
    public Button getCheckBox() {
      return checkBox;
    }
    
    public IssueStatus getIssueStatus() {
      return issueStatus;
    }
  }
}
