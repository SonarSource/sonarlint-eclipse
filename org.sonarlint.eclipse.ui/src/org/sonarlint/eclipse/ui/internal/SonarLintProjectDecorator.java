/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintProjectDecorator implements ILightweightLabelDecorator {

  public static final String ID = "org.sonarlint.eclipse.ui.sonarlintDecorator";

  private final ListenerList<ILabelProviderListener> fListeners = new ListenerList<>();

  @Override
  public void decorate(Object element, IDecoration decoration) {
    var project = SonarLintUtils.adapt(element, ISonarLintProject.class,
      "[SonarLintProjectDecorator#decorate] Try get project of object '" + element + "'");
    if (project != null && project.isOpen()) {
      var config = SonarLintCorePlugin.loadConfig(project);
      if (!config.isAutoEnabled()) {
        return;
      }
      SonarLintCorePlugin.getConnectionManager().resolveBinding(project)
        .ifPresent(s -> decoration.addOverlay(SonarLintImages.SQ_LABEL_DECORATOR));
    }
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    fListeners.add(listener);
  }

  @Override
  public void dispose() {
    var listeners = fListeners.getListeners();
    for (Object listener : listeners) {
      fListeners.remove(listener);
    }
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    fListeners.remove(listener);
  }

  public void fireChange(Collection<ISonarLintProject> elements) {
    if (!fListeners.isEmpty()) {
      var event = new LabelProviderChangedEvent(this, elements.stream().map(ISonarLintProject::getObjectToNotify).toArray());
      var listeners = fListeners.getListeners();
      for (Object listener : listeners) {
        ((ILabelProviderListener) listener).labelProviderChanged(event);
      }
    }
  }

}
