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

package org.sonar.ide.eclipse.internal.ui.preferences;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.internal.ui.wizards.EditServerLocationWizard;
import org.sonar.ide.eclipse.internal.ui.wizards.NewServerLocationWizard;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.SelectionUtils;
import org.sonar.wsclient.Host;

/**
 * Preference page for the workspace.
 */
public class SonarPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private static final String PREF_SYNCHRONISE_PROFILE = "synchroniseProfile";
  
  private TableViewer serversViewer;
  private ArrayList<Button> checkBoxes;

  private List<Host> servers;

  public SonarPreferencePage() {
    super(Messages.getString("pref.global.title")); //$NON-NLS-1$
    noDefaultAndApplyButton();
    checkBoxes = new ArrayList<Button>();
  }

  public void init(IWorkbench workbench) {
    setDescription("Add, remove or edit Sonar servers:");
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    createServersTable(container);
    initServersTable();
    createStartupPreferences(container);

    return container;
  }

  private Host getSelectedServer() {
    return (Host) SelectionUtils.getSingleElement(serversViewer.getSelection());
  }

  private void initServersTable() {
    // retrieve list of servers
    servers = SonarCorePlugin.getServersManager().getHosts();
    serversViewer.setInput(servers);
  }

  private void createServersTable(Composite composite) {
    serversViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
    serversViewer.setContentProvider(ArrayContentProvider.getInstance());
    serversViewer.setLabelProvider(new ServersLabelProvider());

    final Table table = serversViewer.getTable();
    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 3);
    gridData.heightHint = 300;
    table.setLayoutData(gridData);

    final Button addButton = new Button(composite, SWT.NONE);
    final Button editButton = new Button(composite, SWT.NONE);
    final Button removeButton = new Button(composite, SWT.NONE);

    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText(Messages.getString("action.add.server")); //$NON-NLS-1$
    addButton.setToolTipText(Messages.getString("action.add.server.desc")); //$NON-NLS-1$
    addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    addButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        NewServerLocationWizard wiz = new NewServerLocationWizard();
        wiz.init(SonarUiPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(addButton.getShell(), wiz);
        dialog.create();
        if (dialog.open() == Window.OK) {
          initServersTable();
        }
      }
    });

    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setText(Messages.getString("action.edit.server")); //$NON-NLS-1$
    editButton.setToolTipText(Messages.getString("action.edit.server.desc")); //$NON-NLS-1$
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
          initServersTable();
        }
        removeButton.setEnabled(false);
        editButton.setEnabled(false);
      }
    });

    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removeButton.setText(Messages.getString("action.delete.server")); //$NON-NLS-1$
    removeButton.setToolTipText(Messages.getString("action.delete.server.desc")); //$NON-NLS-1$
    removeButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());
    removeButton.setEnabled(false);
    removeButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        Host selected = getSelectedServer();
        if (MessageDialog.openConfirm(SonarPreferencePage.this.getShell(), Messages.getString("remove.server.dialog.caption"), //$NON-NLS-1$
            MessageFormat.format(Messages.getString("remove.server.dialog.msg"), //$NON-NLS-1$
                new Object[] { selected.getHost() }))) {
          SonarCorePlugin.getServersManager().removeServer(selected.getHost());
          servers.remove(selected);
          serversViewer.refresh();
          removeButton.setEnabled(false);
          editButton.setEnabled(false);
        }
      }
    });

    serversViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
        removeButton.setEnabled( !servers.isEmpty());
        editButton.setEnabled(true);
      }
    });
  }

  private class ServersLabelProvider extends AbstractTableLabelProvider {

    @Override
    public String getColumnText(Object element, int columnIndex) {
      Host host = (Host) element;
      return host.getHost();
    }
  }

  private void createStartupPreferences(Composite composite) {
    Group group = new Group(composite, SWT.NONE);
    group.setLayout(new GridLayout());
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    group.setText("Quality profile");

    addCheckBox(group, "Synchronise profile with eclipse formatter at startup", PREF_SYNCHRONISE_PROFILE);
  }

  private Button addCheckBox(Composite parent, String label, String key) {
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);

    Button button = new Button(parent, SWT.CHECK);
    button.setText(label);
    button.setData(key);
    button.setLayoutData(gd);

    button.setSelection(SonarUiPlugin.getDefault().getPreferenceStore().getBoolean(key));

    checkBoxes.add(button);
    return button;
  }

  public boolean performOk() {
    IPreferenceStore store = SonarUiPlugin.getDefault().getPreferenceStore();
    for (Button button : checkBoxes) {
      String key = (String) button.getData();
      store.setValue(key, button.getSelection());
    }
    return super.performOk();
  }
}
