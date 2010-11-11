package org.sonar.ide.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.eclipse.core.internal.SonarFile;
import org.sonar.ide.eclipse.core.internal.SonarMeasure;
import org.sonar.ide.eclipse.core.internal.SonarResource;
import org.sonar.wsclient.services.Measure;

public class SonarCorePlugin extends Plugin {
  private static SonarCorePlugin plugin;

  public static SonarCorePlugin getDefault() {
    return plugin;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  public static ISonarResource createSonarResource(IResource resource, String key, String name) {
    return new SonarResource(resource, key, name);
  }

  public static ISonarFile createSonarFile(IFile file, String key, String name) {
    return new SonarFile(file, key, name);
  }

  public static ISonarMeasure createSonarMeasure(ISonarResource sonarResource, Measure measure) {
    return new SonarMeasure(sonarResource, measure);
  }

  public static ISonarMeasure createSonarMeasure(ISonarResource sonarResource, IMeasure measure) {
    return new SonarMeasure(sonarResource, measure.getMetricDef().getKey(), measure.getMetricDef().getName(), measure.getValue());
  }
}
