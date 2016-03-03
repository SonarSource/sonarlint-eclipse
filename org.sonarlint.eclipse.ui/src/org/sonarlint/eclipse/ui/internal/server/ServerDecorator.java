package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.sonarlint.eclipse.core.internal.server.IServer;

public class ServerDecorator extends LabelProvider implements ILightweightLabelDecorator {

  static final String ID = "org.sonarlint.eclipse.ui.navigatorDecorator";

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IServer) {
      IServer server = (IServer) element;

      switch (server.getState()) {
        case STOPPED:
          addSuffix(decoration, "Stopped");
          break;
        case STARTED_NOT_SYNCED:
          addSuffix(decoration, "Not synced");
          break;
        case STARTED_SYNCED:
          addSuffix(decoration, "Version: " + server.getServerVersion() + ", Last sync: " + server.getSyncDate());
          break;
        case STARTING:
          addSuffix(decoration, "Starting...");
          break;
        case SYNCING:
          addSuffix(decoration, "Synchonizing...");
          break;
        default:
          throw new IllegalArgumentException(server.getState().name());
      }

    }
  }

  private void addSuffix(IDecoration decoration, String suffix) {
    decoration.addSuffix(" [" + suffix + "]");
  }

}
