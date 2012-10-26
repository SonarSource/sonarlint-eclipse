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
package org.sonar.batch.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.Logs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EmbedderMetricFinder implements MetricFinder {

  private final Map<String, Metric> metrics = Maps.newHashMap();

  public EmbedderMetricFinder(Metric[] metrics) {
    for (Metric metric : metrics) {
      this.metrics.put(metric.getKey(), metric);
    }
    Logs.INFO.info("Registered " + metrics.length + " metrics");
  }

  public Collection<Metric> findAll() {
    return Lists.newArrayList(metrics.values());
  }

  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = Lists.newArrayList();
    for (String key : metricKeys) {
      Metric metric = findByKey(key);
      if (metric != null) {
        result.add(metric);
      }
    }
    return result;
  }

  public Metric findById(int id) {
    throw new EmbedderUnsupportedOperationException("Searching metric by id doesn't make sense without database");
  }

  public Metric findByKey(String key) {
    return metrics.get(key);
  }

}
