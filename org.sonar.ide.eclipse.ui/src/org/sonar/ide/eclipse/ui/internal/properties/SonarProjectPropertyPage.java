/*
 * SonarQube Eclipse
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
package org.sonar.ide.eclipse.ui.internal.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.Messages;

import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * Property page for projects to view sonar server connection. It store in
 * <project>/.settings/org.sonar.ide.eclipse.prefs following properties:
 * - url,
 * - groupId, artifactId, branch
 *
 * @author Jérémie Lagarde
 */
public class SonarProjectPropertyPage extends PropertyPage {

  private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

  public SonarProjectPropertyPage() {
    setTitle(Messages.SonarProjectPropertyPage_title);
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    if (parent == null) {
      return new Composite(parent, SWT.NULL);
    }

    final SonarProject sonarProject = SonarProject.getInstance(getProject());
    if (sonarProject == null) {
      return new Composite(parent, SWT.NULL);
    }

    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    container.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    addText(Messages.SonarProjectPropertyBlock_label_host, sonarProject.getUrl(), container);
    addText(Messages.SonarProjectPropertyBlock_label_key, sonarProject.getKey(), container);
    Properties props = new Properties();
    ConfiguratorUtils.configure(sonarProject.getProject(), props, new NullProgressMonitor());
    addText(Messages.SonarProjectPropertyBlock_label_language, props.getProperty(SonarConfiguratorProperties.PROJECT_LANGUAGE_PROPERTY), container);
    if (sonarProject.getLastAnalysisDate() != null) {
      addText(Messages.SonarProjectPropertyBlock_label_analysis_date, sdf.format(sonarProject.getLastAnalysisDate()), container);
    }

    return container;
  }

  private Text addText(String label, String text, Composite container) {
    Label labelField = new Label(container, SWT.NONE);
    labelField.setText(label);

    Text textField = new Text(container, SWT.WRAP | SWT.READ_ONLY);
    textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    textField.setBackground(textField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    textField.setText(text);
    textField.setEditable(false);
    return textField;
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }
}
