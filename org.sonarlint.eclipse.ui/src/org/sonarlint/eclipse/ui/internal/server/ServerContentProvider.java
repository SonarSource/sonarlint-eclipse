package org.sonarlint.eclipse.ui.internal.server;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.server.IServer;

public class ServerContentProvider extends BaseContentProvider implements ITreeContentProvider {
  public static Object INITIALIZING = new Object();

  @Override
  public Object[] getElements(Object element) {
    List<IServer> list = new ArrayList<IServer>();
    IServer[] servers = SonarLintCorePlugin.getDefault().getServers();
    if (servers != null) {
      int size = servers.length;
      for (int i = 0; i < size; i++) {
        list.add(servers[i]);
      }
    }
    return list.toArray();
  }

  @Override
  public Object[] getChildren(Object element) {
    // TODO Associated projects
    return new Object[0];
  }

  @Override
  public Object getParent(Object element) {
    // TODO server of associated project
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof IServer) {
      // TODO check if it has associated projects
      return false;
    }
    return false;
  }
}
