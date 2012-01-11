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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;

public class EmbedderPersistenceManagerTest {

  private EmbedderPersistenceManager persistenceManager;

  @Before
  public void setUp() {
    persistenceManager = new EmbedderPersistenceManager();
  }

  @Test
  public void shouldSave() {
    Resource resource = new JavaFile("org.foo.Bar");
    Measure measure = new Measure("lines");
    persistenceManager.saveMeasure(resource, measure);
    assertThat(persistenceManager.getAllMeasures().size(), is(1));
    assertThat(persistenceManager.getMeasures("org.foo.Bar").size(), is(1));
  }

  @Test(expected = SonarException.class)
  public void shouldNotSaveMeasureTwice() {
    Resource resource = new JavaFile("org.foo.Bar");
    Measure measure = new Measure("lines");
    persistenceManager.saveMeasure(resource, measure);
    persistenceManager.saveMeasure(resource, measure);
  }

  @Test
  public void shouldNotSave() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Scopes.PROGRAM_UNIT);
    Measure measure = new Measure("lines");
    persistenceManager.saveMeasure(resource, measure);
    assertThat(persistenceManager.getAllMeasures().size(), is(0));
  }

  @Test
  public void shouldNotSaveRuleMeasure() {
    Resource resource = new JavaFile("org.foo.Bar");
    RuleMeasure measure = RuleMeasure.createForPriority(new Metric.Builder("violations", "Violations", Metric.ValueType.INT).create(), RulePriority.BLOCKER, 1D);
    persistenceManager.saveMeasure(resource, measure);
    assertThat(persistenceManager.getAllMeasures().size(), is(0));
  }

}
