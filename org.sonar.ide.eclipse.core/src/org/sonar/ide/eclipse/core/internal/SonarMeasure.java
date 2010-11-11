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

package org.sonar.ide.eclipse.core.internal;

import org.eclipse.core.runtime.Assert;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.wsclient.services.Measure;

public class SonarMeasure implements ISonarMeasure {

  private final ISonarResource resource;
  private final String metricKey;
  private final String metricName;
  private final String value;

  public SonarMeasure(ISonarResource resource, Measure measure) {
    this(resource, measure.getMetricKey(), measure.getMetricName(), measure.getFormattedValue());
  }

  public SonarMeasure(ISonarResource resource, String metricKey, String metricName, String value) {
    Assert.isNotNull(resource);
    Assert.isNotNull(metricKey);
    Assert.isNotNull(metricName);
    Assert.isNotNull(value);
    this.resource = resource;
    this.metricKey = metricKey;
    this.metricName = metricName;
    this.value = value;
  }

  public ISonarResource getSonarResource() {
    return resource;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public String getMetricName() {
    return metricName;
  }

  public String getValue() {
    return value;
  }

}
