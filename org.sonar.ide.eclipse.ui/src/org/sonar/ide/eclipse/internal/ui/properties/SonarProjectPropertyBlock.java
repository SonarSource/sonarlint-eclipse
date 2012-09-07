/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.ui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.ui.Messages;

public class SonarProjectPropertyBlock {
  public Control createContents(Composite parent, ProjectProperties properties) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    addText(Messages.SonarProjectPropertyBlock_label_host, properties.getUrl(), container);
    addText(Messages.SonarProjectPropertyBlock_label_groupId, properties.getGroupId(), container);
    addText(Messages.SonarProjectPropertyBlock_label_artifactId, properties.getArtifactId(), container);
    addText(Messages.SonarProjectPropertyBlock_label_branch, properties.getBranch(), container);

    return container;
  }

  private void addText(String label, String text, Composite container) {
    Label labelField = new Label(container, SWT.NULL);
    labelField.setText(label);

    Text textField = new Text(container, SWT.BORDER | SWT.SINGLE);
    textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    textField.setText(text);
    textField.setEditable(false);
  }
}
