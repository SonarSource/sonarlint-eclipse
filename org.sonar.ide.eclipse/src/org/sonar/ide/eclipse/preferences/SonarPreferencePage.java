/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.preferences;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.wizards.EditServerLocationWizard;
import org.sonar.ide.eclipse.wizards.NewServerLocationWizard;
import org.sonar.wsclient.Host;

/**
 * Preference page for the workspace.
 */
public class SonarPreferencePage extends AbstractSonarPreferencePage {

  private CheckboxTableViewer serversViewer;

  private List<Host> servers;
  private Host defaultServer;

  public SonarPreferencePage() {
    super(Messages.getString("pref.global.title")); //$NON-NLS-1$
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    Label link = new Label(container, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    link.setText("Add, remove or edit Sonar servers.");
    createTable(container);

    // retrieve list of servers
    servers = SonarPlugin.getServerManager().getServers();
    // TODO Godin: remove this dirty hack
    if (servers.size() == 0) {
      servers.add(new Host("http://localhost:9000"));
    }
    serversViewer.setInput(servers);

    // set default server
    String defaultServerUrl = getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    for (Host server : servers) {
      if (defaultServerUrl.equals(server.getHost())) {
        setCheckedServer(server);
      }
    }

    // TODO Godin: we need to remove unnecessary buttons, but this doesn't work:
    // getDefaultsButton().setVisible(false);
    // getApplyButton().setVisible(false);

    return container;
  }

  @Override
  protected void performApply() {
    getPreferenceStore().setValue(PreferenceConstants.P_SONAR_SERVER_URL, defaultServer.getHost());
  }

  private Host getSelectedServer() {
    IStructuredSelection selection = (IStructuredSelection) serversViewer.getSelection();
    return (Host) selection.getFirstElement();
  }

  private void setCheckedServer(Host host) {
    serversViewer.setAllChecked(false);
    serversViewer.setChecked(host, true);
    defaultServer = host;
  }

  private void createTable(Composite composite) {
    serversViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
    serversViewer.setContentProvider(new ServersContentProvider());
    serversViewer.setLabelProvider(new ServersLabelProvider());

    Table table = serversViewer.getTable();
    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 3);
    gridData.heightHint = 300;
    table.setLayoutData(gridData);

    serversViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        if (event.getElement() != null && event.getChecked()) {
          setCheckedServer((Host) event.getElement());
        }
      }
    });

    final Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText(Messages.getString("action.add.server")); //$NON-NLS-1$
    addButton.setToolTipText(Messages.getString("action.add.server.desc")); //$NON-NLS-1$
    addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        NewServerLocationWizard wiz = new NewServerLocationWizard();
        wiz.init(SonarPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(addButton.getShell(), wiz);
        dialog.create();
        if (dialog.open() == Window.OK) {
          serversViewer.setInput(SonarPlugin.getServerManager().getServers());
        }
      }
    });

    final Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setText(Messages.getString("action.edit.server")); //$NON-NLS-1$
    editButton.setToolTipText(Messages.getString("action.edit.server.desc")); //$NON-NLS-1$
    editButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    editButton.setEnabled(false);
    editButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Host selected = getSelectedServer();
        EditServerLocationWizard wiz = new EditServerLocationWizard(selected.getHost());
        wiz.init(SonarPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(editButton.getShell(), wiz);
        dialog.create();
        if (dialog.open() == Window.OK) {
          serversViewer.refresh();
        }
      }
    });

    final Button removeButton = new Button(composite, SWT.NONE);
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
          SonarPlugin.getServerManager().removeServer(selected.getHost());
          servers.remove(selected);
          serversViewer.refresh();
          removeButton.setEnabled(false);
          editButton.setEnabled(false);
          // set new deault server, if previous was removed
          Object[] checkedElements = serversViewer.getCheckedElements();
          if (checkedElements == null || checkedElements.length == 0) {
            setCheckedServer(servers.get(0));
          }
        }
      }
    });

    serversViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        removeButton.setEnabled(servers.size() > 1);
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

  private class ServersContentProvider implements IStructuredContentProvider {

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {
      return ((List) inputElement).toArray();
    }
  }

}
