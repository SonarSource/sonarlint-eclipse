package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * Server table label provider.
 */
public class ServerLabelProvider extends BaseCellLabelProvider {

  @Override
  public String getText(Object element) {
    if (element instanceof IServer) {
      IServer server = (IServer) element;
      return notNull(server.getName());
    }

    if (element == ServerContentProvider.INITIALIZING)
      return Messages.viewInitializing;

    if (element instanceof IWorkspaceRoot) {
      return Platform.getResourceString(SonarLintUiPlugin.getDefault().getBundle(), "%viewServers");
    }

    return "";
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IServer) {
      return SonarLintImages.SONAR16_IMG;
    }
    return null;
  }

  protected String notNull(String s) {
    if (s == null)
      return "";
    return s;
  }

  @Override
  public Image getColumnImage(Object element, int index) {
    // TODO Left blank since the CNF doesn't support this
    return null;
  }

  @Override
  public String getColumnText(Object element, int index) {
    // TODO Left blank since the CNF doesn't support this
    return null;
  }

}
