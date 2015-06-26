/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.wsclient.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.ide.eclipse.wsclient.ISonarRemoteModule;
import org.sonar.ide.eclipse.wsclient.ISonarWSClientFacade;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Authentication;
import org.sonar.wsclient.services.AuthenticationQuery;
import org.sonar.wsclient.services.Model;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.ResourceSearchQuery;
import org.sonar.wsclient.services.ResourceSearchResult;
import org.sonar.wsclient.services.ServerQuery;
import org.sonar.wsclient.services.Source;
import org.sonar.wsclient.services.SourceQuery;

public class SonarWSClientFacade implements ISonarWSClientFacade {

  private final Sonar sonar;
  private final SonarClient sonarClient;

  public SonarWSClientFacade(final Sonar sonar, final SonarClient sonarClient) {
    this.sonar = sonar;
    this.sonarClient = sonarClient;
  }

  @Override
  public ConnectionTestResult testConnection() {
    try {
      Authentication auth = sonar.find(new AuthenticationQuery());
      if (auth.isValid()) {
        return new ConnectionTestResult(ConnectionTestStatus.OK);
      } else {
        return new ConnectionTestResult(ConnectionTestStatus.AUTHENTICATION_ERROR);
      }
    } catch (Exception e) {
      return new ConnectionTestResult(ConnectionTestStatus.CONNECT_ERROR, e.getMessage());
    }
  }

  @Override
  public String getServerVersion() {
    return find(new ServerQuery()).getVersion();
  }

  @Override
  public List<ISonarRemoteModule> listAllRemoteModules() {
    ResourceQuery query = new ResourceQuery().setScopes(Resource.SCOPE_SET).setQualifiers(Resource.QUALIFIER_PROJECT, Resource.QUALIFIER_MODULE);
    List<Resource> resources = findAll(query);
    List<ISonarRemoteModule> result = new ArrayList<ISonarRemoteModule>(resources.size());
    for (Resource resource : resources) {
      result.add(new SonarRemoteModule(resource));
    }
    return result;
  }

  private <M extends Model> M find(Query<M> query) {
    try {
      return sonar.find(query);
    } catch (ConnectionException e) {
      throw new org.sonar.ide.eclipse.wsclient.ConnectionException(e);
    }
  }

  private List<Resource> findAll(ResourceQuery query) {
    try {
      return sonar.findAll(query);
    } catch (ConnectionException e) {
      throw new org.sonar.ide.eclipse.wsclient.ConnectionException(e);
    }
  }

  @Override
  public List<ISonarRemoteModule> searchRemoteModules(String text) {
    if (text.length() < 2) {
      return Collections.emptyList();
    }
    List<ISonarRemoteModule> result;
    ResourceSearchQuery query = ResourceSearchQuery.create(text).setQualifiers(Resource.QUALIFIER_PROJECT, Resource.QUALIFIER_MODULE);
    ResourceSearchResult searchResult = find(query);

    result = new ArrayList<ISonarRemoteModule>(searchResult.getResources().size());
    for (ResourceSearchResult.Resource resource : searchResult.getResources()) {
      result.add(new SonarRemoteModule(resource));
    }

    return result;
  }

  @Override
  public boolean exists(String resourceKey) {
    return find(new ResourceQuery().setResourceKeyOrId(resourceKey)) != null;
  }

  @CheckForNull
  @Override
  public String[] getRemoteCode(String resourceKey) {
    Source source = find(SourceQuery.create(resourceKey));
    String[] remote = new String[source.getLinesById().lastKey()];
    for (int i = 0; i < remote.length; i++) {
      remote[i] = source.getLine(i + 1);
      if (remote[i] == null) {
        remote[i] = "";
      }
    }
    return remote;
  }

  private static class SonarRemoteModule implements ISonarRemoteModule {

    private String key;
    private String name;

    public SonarRemoteModule(final Resource resource) {
      this.key = resource.getKey();
      this.name = resource.getName();
    }

    public SonarRemoteModule(final ResourceSearchResult.Resource resource) {
      this.key = resource.key();
      this.name = resource.name();
    }

    @Override
    public String getKey() {
      return this.key;
    }

    @Override
    public String getName() {
      return this.name;
    }
  }

}
