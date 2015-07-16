/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.ui.internal.preferences;

import java.text.MessageFormat;
import java.util.Collection;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.ISonarServersManager;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.util.SelectionUtils;
import org.sonar.ide.eclipse.ui.internal.wizards.EditServerLocationWizard;
import org.sonar.ide.eclipse.ui.internal.wizards.NewServerLocationWizard;

/**
 * Preference page for the workspace.
 */
public class SonarServerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private TableViewer serversViewer;

  private Collection<ISonarServer> servers;

  public SonarServerPreferencePage() {
    super(Messages.SonarServerPreferencePage_title);
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription(Messages.SonarServerPreferencePage_description);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    createTable(container);

    initTable();

    return container;
  }

  private ISonarServer getSelectedServer() {
    return (ISonarServer) SelectionUtils.getSingleElement(serversViewer.getSelection());
  }

  private void initTable() {
    // retrieve list of servers
    ISonarServersManager serversManager = SonarCorePlugin.getServersManager();
    servers = serversManager.getServers();
    serversViewer.setInput(servers);
    for (int i = 0, n = serversViewer.getTable().getColumnCount(); i < n; i++) {
      serversViewer.getTable().getColumn(i).pack();
    }
  }

  private void createTable(Composite composite) {
    serversViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL
      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

    createColumns();

    serversViewer.setContentProvider(ArrayContentProvider.getInstance());

    final Table table = serversViewer.getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 3);
    gridData.heightHint = 300;
    table.setLayoutData(gridData);

    final Button addButton = new Button(composite, SWT.NONE);
    final Button editButton = new Button(composite, SWT.NONE);
    final Button removeButton = new Button(composite, SWT.NONE);

    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText(Messages.SonarServerPreferencePage_action_add);
    addButton.setToolTipText(Messages.SonarServerPreferencePage_action_add_tooltip);
    addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        NewServerLocationWizard wiz = new NewServerLocationWizard();
        wiz.init(SonarUiPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(addButton.getShell(), wiz);
        dialog.create();
        if (dialog.open() == Window.OK) {
          initTable();
        }
      }
    });

    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setText(Messages.SonarServerPreferencePage_action_edit);
    editButton.setToolTipText(Messages.SonarServerPreferencePage_action_edit_tooltip);
    editButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    editButton.setEnabled(false);
    editButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        EditServerLocationWizard wizard = new EditServerLocationWizard(getSelectedServer());
        wizard.init(SonarUiPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(editButton.getShell(), wizard);
        dialog.create();
        if (dialog.open() == Window.OK) {
          initTable();
        }
        removeButton.setEnabled(false);
        editButton.setEnabled(false);
      }
    });

    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removeButton.setText(Messages.SonarServerPreferencePage_action_delete);
    removeButton.setToolTipText(Messages.SonarServerPreferencePage_action_delete_tooltip);
    removeButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());
    removeButton.setEnabled(false);
    removeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ISonarServer selected = getSelectedServer();
        if (MessageDialog.openConfirm(SonarServerPreferencePage.this.getShell(), "Remove SonarQube server connection",
          MessageFormat.format("Confirm removing {0}",
            new Object[] {selected.getUrl()}))) {
          SonarCorePlugin.getServersManager().removeServer(selected);
          servers.remove(selected);
          serversViewer.refresh();
          removeButton.setEnabled(false);
          editButton.setEnabled(false);
        }
      }
    });

    serversViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        removeButton.setEnabled(!servers.isEmpty());
        editButton.setEnabled(true);
      }
    });
  }

  private void createColumns() {
    TableViewerColumn colId = new TableViewerColumn(serversViewer, SWT.NONE);
    colId.getColumn().setText("ID");
    colId.getColumn().setWidth(100);
    colId.getColumn().setResizable(true);
    colId.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ISonarServer sonarServer = (ISonarServer) element;
        return sonarServer.getId();
      }
    });

    TableViewerColumn colUrl = new TableViewerColumn(serversViewer, SWT.NONE);
    colUrl.getColumn().setText("URL");
    colUrl.getColumn().setWidth(100);
    colUrl.getColumn().setResizable(true);
    colUrl.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ISonarServer sonarServer = (ISonarServer) element;
        return sonarServer.getUrl();
      }
    });

    TableViewerColumn colVersion = new TableViewerColumn(serversViewer, SWT.NONE);
    colVersion.getColumn().setText("Version");
    colVersion.getColumn().setWidth(30);
    colVersion.getColumn().setResizable(true);
    colVersion.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ISonarServer sonarServer = (ISonarServer) element;
        return sonarServer.getVersion();
      }
    });

    TableViewerColumn colEnabled = new TableViewerColumn(serversViewer, SWT.NONE);
    colEnabled.getColumn().setText("Disabled");
    colEnabled.getColumn().setWidth(30);
    colEnabled.getColumn().setResizable(true);
    colEnabled.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ISonarServer sonarServer = (ISonarServer) element;
        return "" + sonarServer.disabled();
      }

    });

  }

}
