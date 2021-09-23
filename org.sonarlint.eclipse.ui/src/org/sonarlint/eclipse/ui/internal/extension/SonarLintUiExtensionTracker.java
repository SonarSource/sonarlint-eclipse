/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IFilter;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.ui.quickfixes.IMarkerResolutionEnhancer;

public class SonarLintUiExtensionTracker implements IExtensionChangeHandler {

  private static SonarLintUiExtensionTracker singleInstance = null;

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private final SonarLintEP<IMarkerResolutionEnhancer> markerResolutionEnhancerEp = new SonarLintEP<>("org.sonarlint.eclipse.ui.markerResolutionEnhancer"); //$NON-NLS-1$

  private static class SonarLintEP<G> {

    private final String id;
    private final Collection<G> instances = new ArrayList<>();

    public SonarLintEP(String id) {
      this.id = id;
    }
  }

  private final Collection<SonarLintEP<?>> allEps = Arrays.asList(markerResolutionEnhancerEp);

  private ExtensionTracker tracker;

  private SonarLintUiExtensionTracker() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    tracker = new ExtensionTracker(reg);
    IExtensionPoint[] epArray = allEps.stream().map(ep -> reg.getExtensionPoint(ep.id)).toArray(IExtensionPoint[]::new);
    // initial population
    for (IExtensionPoint ep : epArray) {
      for (IExtension ext : ep.getExtensions()) {
        addExtension(tracker, ext);
      }
    }
    IFilter filter = ExtensionTracker.createExtensionPointFilter(epArray);
    tracker.registerHandler(this, filter);
  }

  public static synchronized SonarLintUiExtensionTracker getInstance() {
    if (singleInstance == null) {
      singleInstance = new SonarLintUiExtensionTracker();
    }
    return singleInstance;
  }

  public static void close() {
    if (singleInstance != null) {
      singleInstance.tracker.close();
      singleInstance.tracker = null;
    }
  }

  @Override
  public void addExtension(IExtensionTracker tracker, IExtension extension) {
    IConfigurationElement[] configs = extension.getConfigurationElements();
    for (final IConfigurationElement element : configs) {
      try {
        instanciateAndRegister(tracker, extension, element);
      } catch (CoreException e) {
        SonarLintLogger.get().error("Unable to load one SonarLint extension", e);
      }
    }
  }

  private void instanciateAndRegister(IExtensionTracker tracker, IExtension extension, final IConfigurationElement element) throws CoreException {
    for (SonarLintEP ep : allEps) {
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
    for (SonarLintEP ep : allEps) {
      if (ep.id.equals(extension.getExtensionPointUniqueIdentifier())) {
        ep.instances.removeAll(Arrays.asList(objects));
        break;
      }
    }
  }

  public Collection<IMarkerResolutionEnhancer> getMarkerResolutionEnhancers() {
    return markerResolutionEnhancerEp.instances;
  }

}
