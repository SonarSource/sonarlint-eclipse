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
package org.sonar.ide.wsclient;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.api.SourceCodeDiffEngine;
import org.sonar.ide.api.SourceCodeSearchEngine;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * EXPERIMENTAL!!!
 * Layer between Sonar IDE and Sonar based on sonar-ws-client :
 * Sonar IDE -> RemoteSonarIndex -> sonar-ws-client -> Sonar
 *
 * @author Evgeny Mandrikov
 * @since 0.2
 */
class RemoteSonarIndex implements SourceCodeSearchEngine {

  private final Host host;
  private final Sonar sonar;
  private final SourceCodeDiffEngine diffEngine;

  /**
   * Only for testing purposes.
   */
  protected RemoteSonarIndex(Host host) {
    this(host, null);
  }

  public RemoteSonarIndex(Host host, SourceCodeDiffEngine diffEngine) {
    this(host, WSClientFactory.create(host), diffEngine);
  }

  private RemoteSonarIndex(Host host, Sonar sonar, SourceCodeDiffEngine diffEngine) {
    this.sonar = sonar;
    this.diffEngine = diffEngine;
    this.host = host;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode search(String key) {
    Resource resource = sonar.find(new ResourceQuery().setResourceKeyOrId(key));
    if (resource == null) {
      return null;
    }
    return new RemoteSourceCode(key).setRemoteSonarIndex(this);
  }

  /**
   * {@inheritDoc}
   */
  public Collection<SourceCode> getProjects() {
    ArrayList<SourceCode> result = Lists.newArrayList();
    for (Resource resource : sonar.findAll(new ResourceQuery())) {
      result.add(new RemoteSourceCode(resource.getKey(), resource.getName()).setRemoteSonarIndex(this));
    }
    return result;
  }

  protected Host getServer() {
    return host;
  }

  protected Sonar getSonar() {
    return sonar;
  }

  protected SourceCodeDiffEngine getDiffEngine() {
    return diffEngine;
  }

  public Map<String, Metric> getMetrics() {
    // TODO Godin: This is not optimal. Would be better to load metrics only once.
    List<Metric> metrics = getSonar().findAll(MetricQuery.all());
    return Maps.uniqueIndex(metrics, new Function<Metric, String>() {
      public String apply(Metric metric) {
        return metric.getKey();
      }
    });
  }

}
