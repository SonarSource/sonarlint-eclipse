/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.markers.FilterConfigurationArea;
import org.eclipse.ui.views.markers.MarkerFieldFilter;

public class IsNewIssueConfigurationArea extends FilterConfigurationArea {

  private int newIssues;
  private Button newIssuesButton;
  private Button otherIssuesButton;

  public IsNewIssueConfigurationArea() {
    super();
  }

  @Override
  public void apply(MarkerFieldFilter filter) {
    ((IsNewIssueFieldFilter) filter).selectedNewIssues = newIssues;
  }

  @Override
  public void createContents(Composite parent) {
    parent.setLayout(new GridLayout(2, false));

    newIssuesButton = new Button(parent, SWT.CHECK);
    newIssuesButton.setText("New issues");
    newIssuesButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateIssueType(IsNewIssueFieldFilter.SHOW_NEW, newIssuesButton.getSelection());
      }
    });

    otherIssuesButton = new Button(parent, SWT.CHECK);
    otherIssuesButton.setText("Other issues");
    otherIssuesButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateIssueType(IsNewIssueFieldFilter.SHOW_OTHER, otherIssuesButton.getSelection());
      }
    });
  }

  void updateIssueType(int constant, boolean enabled) {
    if (enabled) {
      newIssues = constant | newIssues;
    } else {
      newIssues = constant ^ newIssues;
    }
  }

  @Override
  public void initialize(MarkerFieldFilter filter) {
    if (filter != null) {
      newIssues = ((IsNewIssueFieldFilter) filter).selectedNewIssues;

      otherIssuesButton.setSelection((IsNewIssueFieldFilter.SHOW_OTHER & newIssues) > 0);
      newIssuesButton.setSelection((IsNewIssueFieldFilter.SHOW_NEW & newIssues) > 0);
    }
  }

  @Override
  public String getTitle() {
    return "Issue type";
  }

}
