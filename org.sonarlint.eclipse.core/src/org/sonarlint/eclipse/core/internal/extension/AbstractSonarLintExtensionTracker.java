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
package org.sonarlint.eclipse.core.internal.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.sonarlint.eclipse.core.SonarLintLogger;

public abstract class AbstractSonarLintExtensionTracker implements IExtensionChangeHandler {

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  protected static class SonarLintEP<G> {

    private final String id;
    private final Collection<G> instances = new ArrayList<>();

    public SonarLintEP(String id) {
      this.id = id;
    }

    public Collection<G> getInstances() {
      return instances;
    }
  }

  private static final ExtensionTracker TRACKER = new ExtensionTracker(Platform.getExtensionRegistry());
  private Collection<SonarLintEP<?>> allEpsToWatch;

  protected void init(Collection<SonarLintEP<?>> allEpsToWatch) {
    this.allEpsToWatch = allEpsToWatch;
    var reg = Platform.getExtensionRegistry();
    var epArray = allEpsToWatch.stream().map(ep -> reg.getExtensionPoint(ep.id)).toArray(IExtensionPoint[]::new);
    // initial population
    for (var ep : epArray) {
      for (var ext : ep.getExtensions()) {
        addExtension(TRACKER, ext);
      }
    }
    var filter = ExtensionTracker.createExtensionPointFilter(epArray);
    TRACKER.registerHandler(this, filter);
  }

  protected void unregister() {
    TRACKER.unregisterHandler(this);
  }

  public static final void closeTracker() {
    TRACKER.close();
  }

  @Override
  public void addExtension(IExtensionTracker tracker, IExtension extension) {
    var configs = extension.getConfigurationElements();
    for (final var element : configs) {
      try {
        instanciateAndRegister(tracker, extension, element);
      } catch (CoreException e) {
        SonarLintLogger.get().error("Unable to load one SonarLint extension", e);
      }
    }
  }

  private void instanciateAndRegister(IExtensionTracker tracker, IExtension extension, final IConfigurationElement element) throws CoreException {
    for (SonarLintEP ep : allEpsToWatch) {
      if (ep.id.equals(extension.getExtensionPointUniqueIdentifier())) {
        Object instance = element.createExecutableExtension(ATTR_CLASS);
        ep.instances.add(instance);
        // register association between object and extension with the tracker
        tracker.registerObject(extension, instance, IExtensionTracker.REF_WEAK);
        break;
      }
    }
  }

  @Override
  public void removeExtension(IExtension extension, Object[] objects) {
    // stop using objects associated with the removed extension
    for (SonarLintEP ep : allEpsToWatch) {
      if (ep.id.equals(extension.getExtensionPointUniqueIdentifier())) {
        ep.instances.removeAll(List.of(objects));
        break;
      }
    }
  }

}
