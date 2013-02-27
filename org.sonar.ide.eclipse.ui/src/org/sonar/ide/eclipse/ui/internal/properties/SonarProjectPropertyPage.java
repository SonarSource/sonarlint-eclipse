/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
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

  private Button fOverrideWorkspace;
  private Color savedBackground;
  private Text additionalArgsTextField;
  private List<String> extraArgsInitialValue;

  public SonarProjectPropertyPage() {
    setTitle(Messages.SonarProjectPropertyPage_title);
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

    // Additional arguments

    Composite argsPane = new Composite(container, SWT.NONE);

    argsPane.setLayout(new GridLayout());
    GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
    layoutData.horizontalSpan = 2;
    argsPane.setLayoutData(layoutData);

    fOverrideWorkspace = new Button(argsPane, SWT.CHECK);
    fOverrideWorkspace.setText(Messages.SonarProjectPropertyBlock_label_override_workspace_settings);
    fOverrideWorkspace.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        refreshExtraArgs(sonarProject, fOverrideWorkspace.getSelection());
      }
    });

    Label labelField = new Label(argsPane, SWT.NONE);
    labelField.setText(Messages.SonarPreferencePage_label_extra_args);
    additionalArgsTextField = new Text(argsPane, SWT.MULTI | SWT.BORDER | SWT.WRAP);
    additionalArgsTextField.setLayoutData(new GridData(GridData.FILL_BOTH));
    savedBackground = additionalArgsTextField.getBackground();

    extraArgsInitialValue = sonarProject.getExtraArguments();
    refreshExtraArgs(sonarProject, extraArgsInitialValue != null);

    return container;
  }

  private void refreshExtraArgs(SonarProject sonarProject, boolean overrideWorkspace) {
    if (overrideWorkspace) {
      additionalArgsTextField.setEditable(true);
      additionalArgsTextField.setBackground(savedBackground);
      fOverrideWorkspace.setSelection(true);
      additionalArgsTextField.setText(sonarProject.getExtraArguments() != null ? StringUtils.join(sonarProject.getExtraArguments(), "\n") : "");
    }
    else {
      additionalArgsTextField.setEditable(false);
      additionalArgsTextField.setBackground(additionalArgsTextField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      fOverrideWorkspace.setSelection(false);
      additionalArgsTextField.setText(SonarUiPlugin.getDefault().getPreferenceStore().getString(SonarUiPlugin.PREF_EXTRA_ARGS));
    }
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

  @Override
  protected void performDefaults() {
    SonarProject sonarProject = SonarProject.getInstance(getProject());
    refreshExtraArgs(sonarProject, false);
  }

  @Override
  public boolean performOk() {
    SonarProject sonarProject = SonarProject.getInstance(getProject());
    if (fOverrideWorkspace.getSelection()) {
      sonarProject.setExtraArguments(Arrays.asList(StringUtils.split(additionalArgsTextField.getText(), "\r\n")));
    }
    else {
      sonarProject.setExtraArguments(null);
    }
    sonarProject.save();
    return true;
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }
}
