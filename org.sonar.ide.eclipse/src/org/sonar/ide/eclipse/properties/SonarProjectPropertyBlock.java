/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.wsclient.Host;

import java.util.List;

/**
 * @author Jérémie Lagarde
 * 
 */
public class SonarProjectPropertyBlock {

  private final IProject project;
  private Combo serversCombo;
  private Button serverConfigButton;

  private Text projectGroupIdText;
  private Text projectArtifactIdText;
  private Text projectBranchText;

  public SonarProjectPropertyBlock(IProject project) {
    this.project = project;
  }

  public Control createContents(Composite parent, ProjectProperties projectProperties) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    addServerData(container, projectProperties);
    addSeparator(container);
    addProjectData(container, projectProperties);

    return container;
  }

  private void addServerData(Composite container, ProjectProperties projectProperties) {
    // Create group
    Group group = new Group(container, SWT.NONE);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    data.grabExcessHorizontalSpace = true;
    group.setLayoutData(data);
    group.setText(Messages.getString("pref.project.label.host")); //$NON-NLS-1$
    GridLayout gridLayout = new GridLayout(3, false);
    group.setLayout(gridLayout);

    // Create select list of servers.
    serversCombo = new Combo(group, SWT.READ_ONLY);
    List<Host> servers = SonarPlugin.getServerManager().getServers();
    String defaultServer = projectProperties.getUrl();
    int index = -1;
    for (int i = 0; i < servers.size(); i++) {
      Host server = servers.get(i);
      if (StringUtils.equals(defaultServer, server.getHost())) {
        index = i;
      }
      serversCombo.add(server.getHost());
    }
    if (index == -1) {
      serversCombo.add(defaultServer);
      index = servers.size();
    }
    serversCombo.select(index);

    // Create open preference button.
    serverConfigButton = new Button(group, SWT.PUSH);
    serverConfigButton.setText(Messages.getString("action.open.sonar.preference")); //$NON-NLS-1$
    serverConfigButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    serverConfigButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        PreferenceDialog preference = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
            "org.sonar.ide.eclipse.preferences.SonarPreferencePage", null, null);
        if (preference != null && (preference.open() == Window.OK)) {
          serversCombo.removeAll();
          List<Host> servers = SonarPlugin.getServerManager().getServers();
          for (Host server : servers) {
            serversCombo.add(server.getHost());
          }
          serversCombo.select(servers.size() - 1);
        }
      }
    });
  }

  private void addSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.horizontalSpan = 2;
    gridData.grabExcessHorizontalSpace = true;
    separator.setLayoutData(gridData);
  }

  private void addProjectData(Composite container, ProjectProperties projectProperties) {
    // Project groupId
    Label labelGroupId = new Label(container, SWT.NULL);
    labelGroupId.setText(Messages.getString("pref.project.label.groupid")); //$NON-NLS-1$
    projectGroupIdText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectGroupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectGroupIdText.setText(projectProperties.getGroupId());

    // Project artifactId
    Label labelArtifactId = new Label(container, SWT.NULL);
    labelArtifactId.setText(Messages.getString("pref.project.label.artifactid")); //$NON-NLS-1$
    projectArtifactIdText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectArtifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectArtifactIdText.setText(projectProperties.getArtifactId());

    // Project branch
    Label labelBranch = new Label(container, SWT.NULL);
    labelBranch.setText(Messages.getString("pref.project.label.branch")); //$NON-NLS-1$
    projectBranchText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectBranchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectBranchText.setText(projectProperties.getBranch());
  }

  protected String getUrl() {
    return serversCombo.getText();
  }

  protected String getGroupId() {
    return projectGroupIdText.getText();
  }

  protected String getArtifactId() {
    return projectArtifactIdText.getText();
  }

  protected String getBranch() {
    return projectBranchText.getText();
  }
}
