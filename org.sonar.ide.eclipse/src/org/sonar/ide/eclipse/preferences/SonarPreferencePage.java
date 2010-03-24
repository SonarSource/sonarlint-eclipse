package org.sonar.ide.eclipse.preferences;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.wizards.NewServerLocationWizard;
import org.sonar.wsclient.Host;

/**
 * Preference page for the workspace.
 * 
 * @author Jérémie Lagarde
 */
public class SonarPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private Combo  serversCombo;
  private Button createServerButton;

  public SonarPreferencePage() {
    super(Messages.getString("pref.global.title")); //$NON-NLS-1$
  }

  public void init(IWorkbench workbench) {
  }

  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    return SonarPlugin.getDefault().getPreferenceStore();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    addServerGroup(container);

    return container;
  }

  private void addServerGroup(Composite container) {

    // Create group
    Group group = new Group(container, SWT.NONE);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    group.setLayoutData(data);
    group.setText(Messages.getString("pref.global.label.host")); //$NON-NLS-1$
    GridLayout gridLayout = new GridLayout(2, false);
    group.setLayout(gridLayout);

    // Create select list of servers.
    serversCombo = new Combo(group, SWT.NONE);
    List<Host> servers = SonarPlugin.getServerManager().getServers();
    String defaultServer = getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    int index = -1;
    for (int i = 0; i < servers.size(); i++) {
      Host server = servers.get(i);
      if (StringUtils.equals(defaultServer, server.getHost()))
        index = i;
      serversCombo.add(server.getHost());
    }
    if (index == -1) {
      serversCombo.add(defaultServer);
      index = servers.size();
    }
    serversCombo.select(index);

    // Create new server button.
    createServerButton = new Button(group, SWT.PUSH);
    createServerButton.setText(Messages.getString("action.add.server")); //$NON-NLS-1$
    createServerButton.setToolTipText(Messages.getString("action.add.server.desc")); //$NON-NLS-1$
    createServerButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD).createImage());
    createServerButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    createServerButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        NewServerLocationWizard wiz = new NewServerLocationWizard();
        wiz.init(SonarPlugin.getDefault().getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(createServerButton.getShell(), wiz);
        dialog.create();
        if (dialog.open() == Window.OK) {
          System.out.println("test");
          serversCombo.removeAll();
          List<Host> servers = SonarPlugin.getServerManager().getServers();
          for (Host server : servers) {
            serversCombo.add(server.getHost());
          }
          serversCombo.select(servers.size()-1);
        }
      }
    });
  }

  @Override
  public boolean performOk() {
    performApply();
    return super.performOk();
  }

  @Override
  protected void performApply() {
    getPreferenceStore().setValue(PreferenceConstants.P_SONAR_SERVER_URL, serversCombo.getItem(serversCombo.getSelectionIndex()));
  }

}