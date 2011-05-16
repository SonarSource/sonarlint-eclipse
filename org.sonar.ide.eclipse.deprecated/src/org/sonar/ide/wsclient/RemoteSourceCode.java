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
package org.sonar.ide.wsclient;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.api.SourceCodeDiff;
import org.sonar.ide.shared.measures.MeasureData;
import org.sonar.ide.shared.violations.ViolationUtils;
import org.sonar.wsclient.services.*;

import java.util.*;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
class RemoteSourceCode implements SourceCode {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteSourceCode.class);

  private final String key;
  private final String name;
  private RemoteSonarIndex index;

  private String localContent;

  /**
   * Lazy initialization - see {@link #getDiff()}.
   */
  private SourceCodeDiff diff;

  /**
   * Lazy initialization - see {@link #getRemoteContentAsArray()}.
   */
  private String[] remoteContent;

  /**
   * Lazy initialization - see {@link #getChildren()}.
   */
  private Set<SourceCode> children;

  public RemoteSourceCode(String key) {
    this(key, null);
  }

  public RemoteSourceCode(String key, String name) {
    this.key = key;
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  public Set<SourceCode> getChildren() {
    if (children == null) {
      ResourceQuery query = new ResourceQuery().setDepth(1).setResourceKeyOrId(getKey());
      Collection<Resource> resources = index.getSonar().findAll(query);
      children = new HashSet<SourceCode>();
      for (Resource resource : resources) {
        children.add(new RemoteSourceCode(resource.getKey(), resource.getName()).setRemoteSonarIndex(index));
      }
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode setLocalContent(final String content) {
    this.localContent = content;
    return this;
  }

  private String getLocalContent() {
    if (localContent == null) {
      return "";
    }
    return localContent;
  }

  private String[] getRemoteContentAsArray() {
    if (remoteContent == null) {
      remoteContent = SimpleSourceCodeDiffEngine.getLines(getCode());
    }
    return remoteContent;
  }

  public String getRemoteContent() {
    return StringUtils.join(getRemoteContentAsArray(), "\n");
  }

  private SourceCodeDiff getDiff() {
    if (diff == null) {
      diff = index.getDiffEngine().diff(SimpleSourceCodeDiffEngine.split(getLocalContent()), getRemoteContentAsArray());
    }
    return diff;
  }

  /**
   * {@inheritDoc}
   */
  public List<IMeasure> getMeasures() {
    Map<String, Metric> metricsByKey = index.getMetrics();
    Set<String> keys = metricsByKey.keySet();
    String[] metricKeys = keys.toArray(new String[keys.size()]);
    ResourceQuery query = ResourceQuery.createForMetrics(getKey(), metricKeys);
    Resource resource = index.getSonar().find(query);
    List<IMeasure> result = Lists.newArrayList();
    for (Measure measure : resource.getMeasures()) {
      final Metric metric = metricsByKey.get(measure.getMetricKey());
      final String value = measure.getFormattedValue();
      // Hacks around SONAR-1620
      if (!metric.getHidden() && !"DATA".equals(metric.getType()) && StringUtils.isNotBlank(measure.getFormattedValue())) {
        result.add(new MeasureData().setMetricDef(metric).setValue(value));
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public List<Violation> getViolations() {
    LOG.info("Loading violations for {}", getKey());
    final Collection<Violation> violations = index.getSonar().findAll(ViolationQuery.createForResource(getKey()).setIncludeReview(true));
    LOG.info("Loaded {} violations: {}", violations.size(), ViolationUtils.toString(violations));
    return ViolationUtils.convertLines(violations, getDiff());
  }

  /**
   * {@inheritDoc}
   */
  public List<Violation> getViolations2() {
    return getRemoteSonarIndex().getSonar().findAll(new ViolationQuery(key).setDepth(-1).setIncludeReview(true));
  }

  /**
   * {@inheritDoc}
   */
  public List<Rule> getRules() {
    LOG.info("Loading rules for {}", getKey());
    final Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(getKey(), "profile"));
    final Measure measure = resource.getMeasure("profile");
    if (measure == null) {
      return Collections.emptyList();
    }
    final List<Rule> rules = getRemoteSonarIndex().getSonar().findAll(new RuleQuery(/* TODO */"java").setProfile(measure.getData()));
    LOG.info("Loaded {} rules for profile {}", rules.size(), measure.getData());
    return rules;
  }

  private Source getCode() {
    return index.getSonar().find(new SourceQuery(getKey()));
  }

  /**
   * {@inheritDoc}
   */
  public int compareTo(final SourceCode resource) {
    return key.compareTo(resource.getKey());
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj instanceof RemoteSourceCode) && (key.equals(((RemoteSourceCode) obj).key));
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("key", key).toString();
  }

  protected RemoteSourceCode setRemoteSonarIndex(final RemoteSonarIndex index) {
    this.index = index;
    return this;
  }

  protected RemoteSonarIndex getRemoteSonarIndex() {
    return index;
  }
}
