/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.core.internal.SonarFile;
import org.sonar.ide.eclipse.core.internal.SonarMeasure;
import org.sonar.ide.eclipse.core.internal.SonarMetric;
import org.sonar.ide.eclipse.core.internal.SonarResource;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;

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

  public static ISonarMeasure createSonarMeasure(ISonarResource sonarResource, Metric metric, Measure measure) {
    return new SonarMeasure(sonarResource, metric, measure);
  }

  public static ISonarMetric createSonarMetric(String metricKey) {
    return new SonarMetric(new Metric().setKey(metricKey).setName(metricKey));
  }

}
