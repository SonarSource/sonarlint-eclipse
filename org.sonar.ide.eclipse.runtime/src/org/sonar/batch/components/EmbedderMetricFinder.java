/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.batch.components;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import org.codehaus.plexus.util.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;

public class EmbedderMetricFinder implements MetricFinder {

  private Metric[] metrics;

  public EmbedderMetricFinder(Metric[] metrics) {
    this.metrics = metrics;
  }

  public Collection<Metric> findAll() {
    return Lists.newArrayList(metrics);
  }

  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = Lists.newArrayList();
    for (Metric metric : metrics) {
      for (String key : metricKeys) {
        if (StringUtils.equals(key, metric.getKey())) {
          result.add(metric);
        }
      }
    }
    return result;
  }

  public Metric findById(int id) {
    throw new EmbedderUnsupportedOperationException("Searching metric by id doesn't make sense without database");
  }

  public Metric findByKey(String key) {
    for (Metric metric : metrics) {
      if (StringUtils.equals(key, metric.getKey())) {
        return metric;
      }
    }
    return null;
  }

}
