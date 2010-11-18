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

package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;

public class SonarMeasure extends PlatformObject implements ISonarMeasure {

  private final ISonarResource resource;
  private final SonarMetric metric;
  private final Measure measure;

  public SonarMeasure(ISonarResource resource, Measure measure) {
    this(resource, new Metric().setKey(measure.getMetricKey()).setName(measure.getMetricName()), measure);
  }

  public SonarMeasure(ISonarResource resource, Metric metric, Measure measure) {
    Assert.isNotNull(resource, "sonar resource");
    Assert.isNotNull(metric, "metric");
    Assert.isNotNull(measure, "measure");
    this.resource = resource;
    this.metric = new SonarMetric(metric);
    this.measure = measure;
  }

  public ISonarResource getSonarResource() {
    return resource;
  }

  public String getMetricKey() {
    return getMetricDef().getKey();
  }

  public String getMetricName() {
    return getMetricDef().getName();
  }

  public String getMetricDomain() {
    return getMetricDef().getDomain();
  }

  public ISonarMetric getMetricDef() {
    return metric;
  }

  public String getValue() {
    return measure.getFormattedValue();
  }

  public int getTrend() {
    return measure.getTrend() == null ? 0 : measure.getTrend();
  }

  public int getVar() {
    return measure.getVar() == null ? 0 : measure.getVar();
  }

  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (adapter == ISonarMetric.class) {
      return metric;
    } else if (adapter == ISonarResource.class) {
      return resource;
    }
    return super.getAdapter(adapter);
  }
}
