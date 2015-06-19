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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.markers.FilterConfigurationArea;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerSupportConstants;

public class AssigneeConfigurationArea extends FilterConfigurationArea {

  private Combo descriptionCombo;
  private Text descriptionText;

  /**
   * Create new instance of the receiver.
   */
  public AssigneeConfigurationArea() {
    super();
  }

  @Override
  public void apply(MarkerFieldFilter filter) {
    AssigneeFieldFilter desc = (AssigneeFieldFilter) filter;
    if (descriptionCombo.getSelectionIndex() == 0) {
      desc.setContainsModifier(MarkerSupportConstants.CONTAINS_KEY);
    } else {
      desc.setContainsModifier(MarkerSupportConstants.DOES_NOT_CONTAIN_KEY);
    }
    desc.setContainsText(descriptionText.getText());
  }

  @Override
  public void createContents(Composite parent) {
    createDescriptionGroup(parent);
  }

  @Override
  public void initialize(MarkerFieldFilter filter) {
    AssigneeFieldFilter desc = (AssigneeFieldFilter) filter;
    if (desc.getContainsModifier().equals(MarkerSupportConstants.CONTAINS_KEY)) {
      descriptionCombo.select(0);
    } else {
      descriptionCombo.select(1);
    }

    descriptionText.setText(desc.getContainsText());

  }

  /**
   * Create the group for the description filter.
   *
   * @param parent
   */
  private void createDescriptionGroup(Composite parent) {

    Composite descriptionComposite = new Composite(parent, SWT.NONE);
    descriptionComposite.setLayout(new GridLayout(3, false));
    descriptionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label descriptionLabel = new Label(descriptionComposite, SWT.NONE);
    descriptionLabel.setText("Login:");

    descriptionCombo = new Combo(descriptionComposite, SWT.READ_ONLY);
    descriptionCombo.add("contains");
    descriptionCombo.add("doesn't contains");

    // Prevent Esc and Return from closing the dialog when the combo is
    // active.
    descriptionCombo.addTraverseListener(new TraverseListener() {
      @Override
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
          e.doit = false;
        }
      }
    });

    GC gc = new GC(descriptionComposite);
    gc.setFont(JFaceResources.getDialogFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    gc.dispose();

    descriptionText = new Text(descriptionComposite, SWT.SINGLE | SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    data.widthHint = Dialog.convertWidthInCharsToPixels(fontMetrics, 25);
    descriptionText.setLayoutData(data);
  }

  @Override
  public String getTitle() {
    return "Assignee";
  }
}
