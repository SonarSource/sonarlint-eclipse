/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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

import org.junit.Test;
import org.sonar.api.measures.Metric;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class EmbedderMetricFinderTest {
  @Test
  public void test() {
    Metric metric1 = new Metric.Builder("key1", "name1", Metric.ValueType.STRING).create();
    Metric metric2 = new Metric.Builder("key2", "name2", Metric.ValueType.STRING).create();
    Metric metric3 = new Metric.Builder("key3", "name3", Metric.ValueType.STRING).create();
    EmbedderMetricFinder finder = new EmbedderMetricFinder(new Metric[] {metric1, metric2, metric3});

    assertThat(finder.findByKey("key1"), is(metric1));
    assertThat(finder.findByKey("notfound"), nullValue());
    assertThat(finder.findAll(Arrays.asList("key2", "key3")).size(), is(2));
    assertThat(finder.findAll().size(), is(3));
  }
}
