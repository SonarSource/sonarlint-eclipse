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
import java.util.concurrent.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

/**
 * Property page for projects. It store in
 * <project>/.settings/org.sonarlint.eclipse.prefs following properties
 *
 */
public class SonarLintProjectPropertyPage extends PropertyPage {
  private Composite container;
  private Button enabledBtn;
  private Link boundDetails;
  private Link bindLink;

  // Information displayed regarding New Code preference, only displayed if enabled!
  private Label newCodeHeader;
  private Link newCodeInformation;
  private Link newCodeProjectStatus;

  // Information displayed regarding index exclusions coming from sub-plug-ins (e.g. JDT/CDT/M2e/Buildship)
  private Button indexExclusionEnabledBtn;

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

    // Binding information and settings
    enabledBtn = new Button(container, SWT.CHECK);
    enabledBtn.setText("Run SonarQube automatically");
    enabledBtn.setSelection(getProjectConfig().isAutoEnabled());
    var layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    enabledBtn.setLayoutData(layoutData);

    boundDetails = new Link(container, SWT.NONE);
    boundDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    boundDetails.addListener(SWT.Selection,
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.CONNECTED_MODE_BENEFITS, e.display));

    // New Code information
    newCodeHeader = new Label(container, SWT.NONE);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    newCodeHeader.setLayoutData(gd);
    var fontData = newCodeHeader.getFont().getFontData()[0];
    var font = new Font(parent.getShell().getDisplay(),
      new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
    newCodeHeader.setFont(font);

    newCodeInformation = new Link(container, SWT.NONE);
    newCodeInformation.setLayoutData(gd);
    newCodeInformation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final var dialog = PreferencesUtil.createPreferenceDialogOn(container.getShell(), SonarLintPreferencePage.ID, null, null);
        if (dialog.open() == Window.OK) {
          updateNewCodeState();
          container.requestLayout();
        }
      }
    });

    newCodeProjectStatus = new Link(container, SWT.NONE);
    newCodeProjectStatus.setLayoutData(gd);
    newCodeProjectStatus.addListener(SWT.Selection,
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.CONNECTED_MODE_BENEFITS, e.display));

    // The link to update / set-up a binding.
    bindLink = new Link(container, SWT.NONE);
    bindLink.setLayoutData(gd);
    bindLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final var dialog = ProjectBindingWizard.createDialog(container.getShell(), List.of(getProject()));
        if (dialog.open() == Window.OK) {
          updateConnectionState();
          container.requestLayout();
        }
      }
    });

    // Indexing opt-out
    var indexExclusionsHeader = new Label(container, SWT.NONE);
    indexExclusionsHeader.setLayoutData(gd);
    indexExclusionsHeader.setFont(font);
    indexExclusionsHeader.setText("Project indexing based on other Eclipse plug-ins");

    var indexExclusionsInformation = new Link(container, SWT.NONE);
    indexExclusionsInformation.setLayoutData(new GridData(SWT.LEFT, SWT.DOWN, true, false, 2, 1));
    indexExclusionsInformation.setText("SonarQube for Eclipse uses some Eclipse plugins to index your project. "
      + "Depending on the configuration, it relies\non the Eclipse <a>JDT</a>, <a>CDT</a>, <a>M2E</a> (Maven), or "
      + "<a>Buildship</a> (Gradle) plugins to exclude certain files and folders in your\ncompilation or build output "
      + "directories. This improves overall performance and lowers the memory footprint.\n\nOpting out of these "
      + "exclusions may impact performance but can be beneficial in certain cases. Each project\nshould be assessed "
      + "individually.");
    indexExclusionsInformation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        switch (e.text) {
          case "JDT":
            BrowserUtils.openExternalBrowser(SonarLintDocumentation.ECLIPSE_JDT, container.getDisplay());
            break;
          case "CDT":
            BrowserUtils.openExternalBrowser(SonarLintDocumentation.ECLIPSE_CDT, container.getDisplay());
            break;
          case "M2E":
            BrowserUtils.openExternalBrowser(SonarLintDocumentation.ECLIPSE_M2E, container.getDisplay());
            break;
          case "Buildship":
            BrowserUtils.openExternalBrowser(SonarLintDocumentation.ECLIPSE_BUILDSHIP, container.getDisplay());
            break;
          default:
            break;
        }
      }
    });

    // Binding information and settings
    indexExclusionEnabledBtn = new Button(container, SWT.CHECK);
    indexExclusionEnabledBtn.setText("Rely on Eclipse plugins for indexing and exclusions (changing requires a "
      + "restart of the IDE)");
    indexExclusionEnabledBtn.setSelection(getProjectConfig().isIndexingBasedOnEclipsePlugIns());
    enabledBtn.setLayoutData(layoutData);

    updateConnectionState();
    updateNewCodeState();
    container.requestLayout();

    return container;
  }

  private void updateConnectionState() {
    var projectBinding = getProjectConfig().getProjectBinding();
    if (projectBinding.isPresent()) {
      var connectionOpt = SonarLintCorePlugin.getConnectionManager().findById(projectBinding.get().getConnectionId());
      if (connectionOpt.isEmpty()) {
        throw new IllegalStateException("There must be a valid connection to for this project");
      }

      boundDetails
        .setText("Bound to the project '" + projectBinding.get().getProjectKey() + "' to SonarQube "
          + (connectionOpt.get().getOrganization() == null ? "Server" : "Cloud")
          + " on connection '" + serverName(projectBinding.get().getConnectionId()) + "'.");
      bindLink.setText("<a>Change Binding...</a>");
      bindLink.setVisible(true);
    } else {
      boundDetails.setText("Using SonarQube for Eclipse in Connected Mode with SonarQube (Server, Cloud) will offer "
        + "you a lot of benefits. <a>Learn more</a>");
      bindLink.setText("<a>Bind this Eclipse project to SonarQube (Server, Cloud)...</a>");
      bindLink.setVisible(true);
    }
  }

  private void updateNewCodeState() {
    var projectBinding = getProjectConfig().getProjectBinding();
    if (SonarLintGlobalConfiguration.issuesOnlyNewCode()) {
      newCodeHeader.setText("Focus on New Code is enabled");
      newCodeInformation.setText("Only SonarQube markers in new code are shown. Go to the "
        + "<a>SonarQube preferences</a> to change this setting.");

      newCodeProjectStatus.setVisible(true);

      String newCodeDefinition;
      boolean isSupported;
      try {
        var response = SonarLintBackendService.get().getNewCodeDefinition(getProject()).get();
        isSupported = response.isSupported();
        newCodeDefinition = response.getDescription();
        newCodeDefinition = Character.toLowerCase(newCodeDefinition.charAt(0)) + newCodeDefinition.substring(1);
      } catch (ExecutionException | InterruptedException err) {
        throw new IllegalStateException("There must be New Code definition for this project", err);
      }

      if (projectBinding.isPresent() && !isSupported) {
        newCodeProjectStatus.setText("This project is bound, but the " + newCodeDefinition + ".");
      } else if (projectBinding.isPresent()) {
        newCodeProjectStatus.setText("This project is bound and based on the connection settings, any code changed or "
          + "added " + newCodeDefinition + " is considered new code.");
      } else {
        newCodeProjectStatus.setText("This project is not bound. Any code changed or added "
          + newCodeDefinition + " is considered new code.");
      }
    } else {
      newCodeHeader.setText("Focus on New Code is disabled");
      newCodeInformation.setText("SonarQube markers in overall code are shown. Go to the <a>SonarQube preferences</a> "
        + "to change this setting.");

      newCodeProjectStatus.setVisible(false);
    }
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
    indexExclusionEnabledBtn.setEnabled(true);
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    var projectConfig = getProjectConfig();
    projectConfig.setAutoEnabled(enabledBtn.getSelection());
    projectConfig.setIndexingBasedOnEclipsePlugIns(indexExclusionEnabledBtn.getSelection());
    SonarLintCorePlugin.saveConfig(getProject(), projectConfig);
    return super.performOk();
  }

}
