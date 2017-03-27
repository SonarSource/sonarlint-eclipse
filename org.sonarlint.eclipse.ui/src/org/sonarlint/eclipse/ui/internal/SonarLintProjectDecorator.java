/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal;

import java.util.Collection;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintProjectDecorator implements ILightweightLabelDecorator {

  public static final String ID = "org.sonarlint.eclipse.ui.sonarlintDecorator";

  private ListenerList fListeners;

  @Override
  public void decorate(Object element, IDecoration decoration) {
    ISonarLintProject project = Adapters.adapt(element, ISonarLintProject.class);
    if (project != null && project.isOpen()) {
      SonarLintProjectConfiguration p = SonarLintProjectConfiguration.read(project.getScopeContext());
      if (!p.isAutoEnabled()) {
        return;
      }
      if (p.getServerId() != null && ServersManager.getInstance().getServer(p.getServerId()) != null) {
        decoration.addOverlay(SonarLintImages.SQ_LABEL_DECORATOR);
      }
    }
  }

  @Override
  public void addListener(ILabelProviderListener listener) {

    if (fListeners == null) {
      fListeners = new ListenerList();
    }

    fListeners.add(listener);
  }

  @Override
  public void dispose() {
    if (fListeners != null) {
      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        fListeners.remove(listeners[i]);
      }
      fListeners = null;
    }
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    if (fListeners == null) {
      return;
    }

    fListeners.remove(listener);
  }

  public void fireChange(Collection<ISonarLintProject> elements) {
    if (fListeners != null && !fListeners.isEmpty()) {
      LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, elements.stream().map(ISonarLintProject::getObjectToNotify).toArray());
      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
      }
    }
  }

}
