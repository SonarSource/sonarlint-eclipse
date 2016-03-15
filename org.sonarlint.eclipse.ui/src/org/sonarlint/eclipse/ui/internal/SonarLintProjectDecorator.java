package org.sonarlint.eclipse.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.ServersManager;

public class SonarLintProjectDecorator implements ILightweightLabelDecorator {

  @Override
  public void decorate(Object element, IDecoration decoration) {
    IProject project = null;
    if (element instanceof IProject) {
      project = (IProject) element;
    } else if (element instanceof IAdaptable) {
      project = ((IAdaptable) element).getAdapter(IProject.class);
    }
    if (project != null) {
      SonarLintProject p = SonarLintProject.getInstance(project);
      if (!p.isBuilderEnabled()) {
        return;
      }
      if (p.getServerId() != null && ServersManager.getInstance().getServer(p.getServerId()) != null) {
        decoration.addOverlay(SonarLintImages.LABEL_DECORATOR);
      }
    }
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    // Nothing to do
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    // Nothing to do
  }

}
