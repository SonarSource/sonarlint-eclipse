package org.sonarlint.eclipse.core.internal.extension;

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
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectsProvider;

public class SonarLintExtensionTracker implements IExtensionChangeHandler {

  private static final String CONFIGURATOR_EP = "org.sonarlint.eclipse.core.projectConfigurators"; //$NON-NLS-1$
  private static final String PROJECTSPROVIDER_EP = "org.sonarlint.eclipse.core.projectsProvider"; //$NON-NLS-1$
  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private ExtensionTracker tracker;
  private Collection<ProjectConfigurator> configurators = new ArrayList<>();
  private Collection<ISonarLintProjectsProvider> projectsProviders = new ArrayList<>();

  public void start() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    tracker = new ExtensionTracker(reg);
    IExtensionPoint[] allEps = new IExtensionPoint[] {reg.getExtensionPoint(CONFIGURATOR_EP), reg.getExtensionPoint(PROJECTSPROVIDER_EP)};
    IFilter filter = ExtensionTracker.createExtensionPointFilter(allEps);
    tracker.registerHandler(this, filter);
    for (IExtensionPoint ep : allEps) {
      for (IExtension ext : ep.getExtensions()) {
        addExtension(tracker, ext);
      }
    }
  }

  public void close() {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

  @Override
  public void addExtension(IExtensionTracker tracker, IExtension extension) {
    IConfigurationElement[] configs = extension.getConfigurationElements();
    for (final IConfigurationElement element : configs) {
      try {
        Object instance;
        switch (extension.getExtensionPointUniqueIdentifier()) {
          case CONFIGURATOR_EP:
            instance = addConfigurator(element);
            break;
          case PROJECTSPROVIDER_EP:
            instance = addProjectsProvider(element);
            break;
          default:
            throw new IllegalStateException("Unexpected extension point: " + extension.getExtensionPointUniqueIdentifier());
        }

        // register association between object and extension with the tracker
        tracker.registerObject(extension, instance, IExtensionTracker.REF_WEAK);
      } catch (CoreException e) {
        SonarLintLogger.get().error("Unable to load one SonarLint extension", e);
      }
    }
  }

  private Object addConfigurator(IConfigurationElement element) throws CoreException {
    ProjectConfigurator instance = (ProjectConfigurator) element.createExecutableExtension(ATTR_CLASS);
    configurators.add(instance);
    return instance;
  }

  private Object addProjectsProvider(IConfigurationElement element) throws CoreException {
    ISonarLintProjectsProvider instance = (ISonarLintProjectsProvider) element.createExecutableExtension(ATTR_CLASS);
    projectsProviders.add(instance);
    return instance;
  }

  @Override
  public void removeExtension(IExtension extension, Object[] objects) {
    // stop using objects associated with the removed extension
    switch (extension.getExtensionPointUniqueIdentifier()) {
      case CONFIGURATOR_EP:
        configurators.removeAll(Arrays.asList(objects));
        break;
      case PROJECTSPROVIDER_EP:
        projectsProviders.removeAll(Arrays.asList(objects));
        break;
      default:
        throw new IllegalStateException("Unexpected extension point: " + extension.getExtensionPointUniqueIdentifier());
    }

  }

  public Collection<ProjectConfigurator> getConfigurators() {
    return configurators;
  }

  public Collection<ISonarLintProjectsProvider> getProjectsProviders() {
    return projectsProviders;
  }

}
