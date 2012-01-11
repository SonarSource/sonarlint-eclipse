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
package org.sonar.ide.shared.measures;

import org.sonar.ide.api.IMeasure;
import org.sonar.wsclient.services.Metric;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public final class MeasureData implements IMeasure {

  private Metric metric;
  private String value;

  /**
   * {@inheritDoc}
   */
  public Metric getMetricDef() {
    return metric;
  }

  public MeasureData setMetricDef(Metric metric) {
    this.metric = metric;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public String getValue() {
    return value;
  }

  public MeasureData setValue(String value) {
    this.value = value;
    return this;
  }

}
