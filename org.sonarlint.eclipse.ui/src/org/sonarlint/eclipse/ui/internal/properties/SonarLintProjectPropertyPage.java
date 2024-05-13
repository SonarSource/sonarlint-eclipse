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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.List;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

/**
 * Property page for projects. It store in
 * <project>/.settings/org.sonarlint.eclipse.prefs following properties
 *
 */
public class SonarLintProjectPropertyPage extends PropertyPage {

  private Button enabledBtn;
  private Link addServerLink;
  private Link bindLink;
  private Composite container;
  private Link boundDetails;

  public SonarLintProjectPropertyPage() {
    setTitle(Messages.SonarProjectPropertyPage_title);
  }

  public ISonarLintProject getProject() {
    return SonarLintUtils.adapt(getElement(), ISonarLintProject.class,
      "[SonarLintProjectPropertyPage#getProject] Try get project of preference page '" + getElement().toString() + "'");
  }

  public SonarLintProjectConfiguration getProjectConfig() {
    return SonarLintCorePlugin.loadConfig(getProject());
  }

  @Override
  protected Control createContents(Composite parent) {
    if (parent == null) {
      return new Composite(parent, SWT.NULL);
    }

    container = new Composite(parent, SWT.NULL);
    var layout = new GridLayout();
    container.setLayout(layout);
    container.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
    layout.numColumns = 2;
    layout.verticalSpacing = 9;
    // According to Javadoc of PreferencePage#createContents, child layout must have 0-width margins
    layout.marginHeight = 0;
    layout.marginWidth = 0;

    enabledBtn = new Button(container, SWT.CHECK);
    enabledBtn.setText("Run SonarLint automatically");
    enabledBtn.setSelection(getProjectConfig().isAutoEnabled());
    var layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    enabledBtn.setLayoutData(layoutData);

    boundDetails = new Link(container, SWT.NONE);
    boundDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    boundDetails.addListener(SWT.Selection,
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.CONNECTED_MODE_BENEFITS, e.display));

    addServerLink = new Link(container, SWT.NONE);
    var gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    gd.horizontalSpan = 2;
    addServerLink.setLayoutData(gd);
    addServerLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        var connectionId = getProjectConfig().getProjectBinding().map(EclipseProjectBinding::getConnectionId)
          .orElseThrow(() -> new IllegalStateException("This link should only be visible when there is a serverId"));
        var wd = ServerConnectionWizard.createDialog(container.getShell(), connectionId);
        if (wd.open() == Window.OK) {
          updateState();
        }
      }
    });

    bindLink = new Link(container, SWT.NONE);
    gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    gd.horizontalSpan = 2;
    bindLink.setLayoutData(gd);
    bindLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final var dialog = ProjectBindingWizard.createDialog(container.getShell(), List.of(getProject()));
        if (dialog.open() == Window.OK) {
          updateState();
        }
      }
    });

    updateState();

    return container;
  }

  private void updateState() {
    var projectBinding = getProjectConfig().getProjectBinding();
    if (projectBinding.isPresent()) {
      boundDetails
        .setText("Bound to the project '" + projectBinding.get().getProjectKey() + "' on connection '" + serverName(projectBinding.get().getConnectionId()) + "'");
      bindLink.setText("<a>Update project binding</a>");
    } else {
      boundDetails.setText("Using SonarLint in Connected Mode with SonarQube/SonarCloud will offer you a lot of benefits. <a>Learn more</a>");
      bindLink.setText("<a>Bind this Eclipse project to SonarQube/SonarCloud...</a>");
    }
    if (projectBinding.isPresent() && SonarLintCorePlugin.getConnectionManager().resolveBinding(getProject()).isEmpty()) {
      addServerLink.setText("<a>Re-create SonarQube/SonarCloud connection '" + projectBinding.get().getConnectionId() + "'</a>");
      addServerLink.setVisible(true);
    } else {
      addServerLink.setVisible(false);
    }
    container.requestLayout();
  }

  private static String serverName(final String connectionId) {
    if (connectionId == null) {
      return "";
    }
    var connection = SonarLintCorePlugin.getConnectionManager().findById(connectionId);
    return connection.map(ConnectionFacade::getId).orElseGet(() -> "Unknown connection: '" + connectionId + "'");
  }

  @Override
  protected void performDefaults() {
    enabledBtn.setEnabled(true);
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    var projectConfig = getProjectConfig();
    projectConfig.setAutoEnabled(enabledBtn.getSelection());
    SonarLintCorePlugin.saveConfig(getProject(), projectConfig);
    return super.performOk();
  }

}
