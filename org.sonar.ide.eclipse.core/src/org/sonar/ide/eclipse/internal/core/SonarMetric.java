/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.runtime.Assert;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.wsclient.services.Metric;

public class SonarMetric implements ISonarMetric {

  private final Metric metric;

  public SonarMetric(Metric metric) {
    Assert.isNotNull(metric.getKey(), "metric key");
    Assert.isNotNull(metric, "metric");
    this.metric = metric;
  }

  public String getKey() {
    return metric.getKey();
  }

  public String getName() {
    return metric.getName();
  }

  public String getDomain() {
    return metric.getDomain();
  }

  public String getDescription() {
    return metric.getDescription();
  }

  @Override
  public int hashCode() {
    return getKey().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SonarMetric) && (getKey().equals(((SonarMetric) obj).getKey()));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [key=" + getKey() + "]";
  }

}
