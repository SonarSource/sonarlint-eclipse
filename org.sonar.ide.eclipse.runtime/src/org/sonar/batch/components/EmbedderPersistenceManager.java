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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.Event;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.PersistenceManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmbedderPersistenceManager implements PersistenceManager, EmbedderIndex {

  private Map<String, Map<String, Measure>> measuresByResource = Maps.newHashMap();
  private Map<String, List<Violation>> violationsByResource = Maps.newHashMap();

  public Collection<Measure> getMeasures(String resourceKey) {
    if (!measuresByResource.containsKey(resourceKey)) {
      return Collections.emptyList();
    }
    Map<String, Measure> measuresByMetric = measuresByResource.get(resourceKey);
    return measuresByMetric.values();
  }

  /**
   * For test.
   */
  public Map<String, Map<String, Measure>> getAllMeasures() {
    return measuresByResource;
  }

  public Collection<Violation> getViolations(String resourceKey) {
    if (!violationsByResource.containsKey(resourceKey)) {
      return Collections.emptyList();
    }
    return violationsByResource.get(resourceKey);
  }

  /**
   * @see EmbedderViolationsDecorator
   */
  public void saveViolation(Violation violation) {
    Resource resource = violation.getResource();
    String resourceKey = resource.getKey();
    final List<Violation> violations;
    if (violationsByResource.containsKey(resourceKey)) {
      violations = violationsByResource.get(resourceKey);
    } else {
      violations = Lists.newArrayList();
      violationsByResource.put(resourceKey, violations);
    }
    violations.add(violation);
  }

  public void clear() {
  }

  public void setDelayedMode(boolean b) {
  }

  public void dump() {
  }

  public void saveProject(Project project, Project parent) {
  }

  public Snapshot saveResource(Project project, Resource resource, Resource parent) {
    return null;
  }

  public void setSource(Resource file, String source) {
  }

  public void saveMeasure(Resource resource, Measure measure) {
    if (!ResourceUtils.isPersistable(resource)) {
      return;
    }
    if (measure instanceof RuleMeasure) {
      return; // See SONARIDE-252
    }
    String resourceKey = resource.getKey();
    final Map<String, Measure> measuresByMetric;
    if (measuresByResource.containsKey(resourceKey)) {
      measuresByMetric = measuresByResource.get(resourceKey);
    } else {
      measuresByMetric = Maps.newHashMap();
      measuresByResource.put(resourceKey, measuresByMetric);
    }
    if (measuresByMetric.containsKey(measure.getMetricKey())) {
      throw new SonarException("Can not add twice the same measure on " + resource + ": " + measure);
    }
    measuresByMetric.put(measure.getMetricKey(), measure);
  }

  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
  }

  public void saveLink(Project project, ProjectLink link) {
  }

  public void deleteLink(Project project, String key) {
  }

  public List<Event> getEvents(Resource resource) {
    return null;
  }

  public void deleteEvent(Event event) {
  }

  public void saveEvent(Resource resource, Event event) {
  }

  public Measure reloadMeasure(Measure measure) {
    return measure;
  }

}
