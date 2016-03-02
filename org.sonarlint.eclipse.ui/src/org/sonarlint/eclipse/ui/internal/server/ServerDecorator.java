package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.sonarlint.eclipse.core.internal.server.IServer;

public class ServerDecorator extends LabelProvider implements ILightweightLabelDecorator {

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IServer) {
      IServer server = (IServer) element;

      decoration.addSuffix(" [" + server.getSyncState() + "]");
    }
  }

  public void redecorate(IServer server) {
    fireLabelProviderChanged(new LabelProviderChangedEvent(this));
  }

}
