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
package org.sonar.ide.eclipse.internal.mylyn.ui.wizard;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarClient;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarQuery;
import org.sonar.ide.eclipse.internal.mylyn.ui.Messages;

public class SonarQueryPage extends AbstractRepositoryQueryPage {

  private IRepositoryQuery query;
  private Text titleText;
  private Text projectText;
  private Combo reporterCombo;
  private Text reporterText;
  private Combo assigneeCombo;
  private Text assigneeText;
  private List statusList;
  private List severityList;

  public SonarQueryPage(TaskRepository repository, IRepositoryQuery query) {
    super(Messages.SonarQueryPage_Title, repository, query);
    setTitle(Messages.SonarQueryPage_Title);
    setDescription(Messages.SonarQueryPage_Description); // FIXME
    this.query = query;
  }

  public void createControl(Composite parent) {
    Composite control = new Composite(parent, SWT.NONE);
    control.setLayoutData(new GridData(GridData.FILL_BOTH));
    control.setLayout(new GridLayout(3, false));

    Label titleLabel = new Label(control, SWT.NONE);
    titleLabel.setText(Messages.SonarQueryPage_Query_Title);
    titleText = new Text(control, SWT.BORDER);
    titleText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    titleText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        getContainer().updateButtons();
      }
    });

    // TODO i18n:

    Label projectLabel = new Label(control, SWT.NONE);
    projectLabel.setText("Project Key:");
    projectText = new Text(control, SWT.BORDER);
    projectText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

    Label reporterLabel = new Label(control, SWT.NONE);
    reporterLabel.setText("Created By:");
    reporterCombo = new Combo(control, SWT.READ_ONLY | SWT.BORDER);
    reporterCombo.setItems(new String[] { "Any", "Current user", "Specified user" });
    reporterText = new Text(control, SWT.BORDER);
    reporterText.setEnabled(false);
    reporterText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    reporterCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        reporterText.setEnabled(reporterCombo.getSelectionIndex() == 2);
      }
    });

    Label assigneeLabel = new Label(control, SWT.NONE);
    assigneeLabel.setText("Assigned To:");
    assigneeCombo = new Combo(control, SWT.READ_ONLY | SWT.BORDER);
    assigneeCombo.setItems(new String[] { "Any", "Unassigned", "Current user", "Specified user" });
    assigneeText = new Text(control, SWT.BORDER);
    assigneeText.setEnabled(false);
    assigneeText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    assigneeCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        assigneeText.setEnabled(assigneeCombo.getSelectionIndex() == 3);
      }
    });

    // TODO better layout:
    Label statusLabel = new Label(control, SWT.NONE);
    statusLabel.setText("Status:");
    statusList = new List(control, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
    statusList.setItems(new String[] { SonarClient.STATUS_OPEN, SonarClient.STATUS_REOPENED, SonarClient.STATUS_RESOLVED, SonarClient.STATUS_CLOSED });
    statusList.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false, 2, 1));

    Label severityLabel = new Label(control, SWT.NONE);
    severityLabel.setText("Severity:");
    severityList = new List(control, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
    severityList.setItems(new String[] { SonarClient.PRIORITY_BLOCKER, SonarClient.PRIORITY_CRITICAL, SonarClient.PRIORITY_MAJOR, SonarClient.PRIORITY_MINOR, SonarClient.PRIORITY_INFO });
    severityList.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false, 2, 1));

    if (query != null) {
      // Set values from query
      titleText.setText(query.getSummary());
      projectText.setText(query.getAttribute(SonarQuery.PROJECT));
      select(reporterCombo, query.getAttribute(SonarQuery.REPORTER));
      reporterText.setText(query.getAttribute(SonarQuery.REPORTER_USER));
      select(assigneeCombo, query.getAttribute(SonarQuery.ASSIGNEE));
      assigneeText.setText(query.getAttribute(SonarQuery.ASSIGNEE_USER));
      select(statusList, SonarQuery.getStatuses(query));
      select(severityList, SonarQuery.getSeverities(query));
    } else {
      // Set default values
      reporterCombo.select(0);
      assigneeCombo.select(2);
      statusList.selectAll();
      severityList.selectAll();
    }

    Dialog.applyDialogFont(control);
    setControl(control);
  }

  private static void select(Combo combo, String value) {
    for (int i = 0; i < combo.getItemCount(); i++) {
      if (value.equals(combo.getItem(i))) {
        combo.select(i);
        return;
      }
    }
  }

  private static void select(List list, String[] elements) {
    for (String element : elements) {
      for (int i = 0; i < list.getItemCount(); i++) {
        if (element.equals(list.getItem(i))) {
          list.select(i);
        }
      }
    }
  }

  @Override
  public String getQueryTitle() {
    return titleText.getText();
  }

  @Override
  public void applyTo(IRepositoryQuery query) {
    query.setSummary(getQueryTitle());
    query.setAttribute(SonarQuery.PROJECT, projectText.getText());
    query.setAttribute(SonarQuery.REPORTER, reporterCombo.getText());
    query.setAttribute(SonarQuery.REPORTER_USER, reporterText.getText());
    query.setAttribute(SonarQuery.ASSIGNEE, assigneeCombo.getText());
    query.setAttribute(SonarQuery.ASSIGNEE_USER, assigneeText.getText());
    query.setAttribute(SonarQuery.STATUSES, StringUtils.join(statusList.getSelection(), ','));
    query.setAttribute(SonarQuery.SEVERITIES, StringUtils.join(severityList.getSelection(), ','));
  }

}
