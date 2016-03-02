package org.sonarlint.eclipse.ui.internal.server;

import java.util.Iterator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.server.actions.GlobalDeleteAction;
import org.sonarlint.eclipse.ui.internal.server.actions.GlobalEditAction;
import org.sonarlint.eclipse.ui.internal.server.actions.NewServerWizardAction;

public class ServerActionProvider extends CommonActionProvider {
  public static final String NEW_MENU_ID = "org.eclipse.wst.server.ui.internal.cnf.newMenuId";
  public static final String SHOW_IN_MENU_ID = "org.eclipse.ui.navigate.showInQuickMenu";
  public static final String TOP_SECTION_START_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.topSectionStart";
  public static final String TOP_SECTION_END_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.topSectionEnd";
  public static final String EDIT_SECTION_START_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnfeditSectionStart";
  public static final String EDIT_SECTION_END_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.editSectionEnd";
  public static final String CONTROL_SERVER_SECTION_START_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.controlServerSectionStart";
  public static final String CONTROL_SERVER_SECTION_END_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.controlServerSectionEnd";
  public static final String SERVER_ETC_SECTION_START_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.serverEtcSectionStart";
  public static final String SERVER_ETC_SECTION_END_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.serverEtcSectionEnd";
  public static final String CONTROL_MODULE_SECTION_START_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.controlModuleSectionStart";
  public static final String CONTROL_MODULE_SECTION_END_SEPARATOR = "org.eclipse.wst.server.ui.internal.cnf.controlModuleSectionEnd";

  private ICommonActionExtensionSite actionSite;
  protected Action globalDeleteAction;
  protected Action globalEditAction;

  public ServerActionProvider() {
    super();
  }

  @Override
  public void init(ICommonActionExtensionSite aSite) {
    super.init(aSite);
    this.actionSite = aSite;
    ICommonViewerSite site = aSite.getViewSite();
    if (site instanceof ICommonViewerWorkbenchSite) {
      StructuredViewer v = aSite.getStructuredViewer();
      if (v instanceof CommonViewer) {
        CommonViewer cv = (CommonViewer) v;
        ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
        addListeners(cv);
        makeServerActions(cv, wsSite.getSelectionProvider());
      }
    }
  }

  private void addListeners(CommonViewer tableViewer) {
    tableViewer.addOpenListener(new IOpenListener() {
      @Override
      public void open(OpenEvent event) {
        IStructuredSelection sel = (IStructuredSelection) event.getSelection();
        Object data = sel.getFirstElement();
        if (!(data instanceof IServer))
          return;
        IServer server = (IServer) data;
        SonarLintUiPlugin.getDefault().editServer(server);
      }
    });
  }

  private void makeServerActions(CommonViewer tableViewer, ISelectionProvider provider) {
    Shell shell = tableViewer.getTree().getShell();
    globalDeleteAction = new GlobalDeleteAction(shell, provider);
    globalEditAction = new GlobalEditAction(shell, provider);
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    actionBars.updateActionBars();
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), globalDeleteAction);
    actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), globalEditAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    // This is a temp workaround to clean up the default group that are provided by CNF
    menu.removeAll();

    ICommonViewerSite site = actionSite.getViewSite();
    IStructuredSelection selection = null;
    Shell shell = actionSite.getViewSite().getShell();
    if (site instanceof ICommonViewerWorkbenchSite) {
      ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
      selection = (IStructuredSelection) wsSite.getSelectionProvider().getSelection();
    }

    IServer server = null;
    if (selection != null && !selection.isEmpty()) {
      Iterator iterator = selection.iterator();
      Object obj = iterator.next();
      if (obj instanceof IServer) {
        server = (IServer) obj;
      }
      if (iterator.hasNext()) {
        server = null;
      }
    }

    menu.add(invisibleSeparator(TOP_SECTION_START_SEPARATOR));
    addTopSection(menu);
    menu.add(invisibleSeparator(TOP_SECTION_END_SEPARATOR));
    menu.add(new Separator());

    if (server != null) {
      menu.add(invisibleSeparator(EDIT_SECTION_START_SEPARATOR));
      menu.add(globalEditAction);
      menu.add(globalDeleteAction);
    }
  }

  protected void addTopSection(IMenuManager menu) {
    MenuManager newMenu = new MenuManager(Messages.actionNew, NEW_MENU_ID);
    IAction newServerAction = new NewServerWizardAction();
    newServerAction.setText(Messages.actionNewServer);
    newMenu.add(newServerAction);
    menu.add(newMenu);
  }

  private Separator invisibleSeparator(String s) {
    Separator sep = new Separator(s);
    sep.setVisible(false);
    return sep;
  }
}
