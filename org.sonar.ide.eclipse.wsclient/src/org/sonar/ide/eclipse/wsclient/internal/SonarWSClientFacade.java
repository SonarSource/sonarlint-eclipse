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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.common.issues.ISonarIssueWithPath;
import org.sonar.ide.eclipse.wsclient.ISonarRemoteModule;
import org.sonar.ide.eclipse.wsclient.ISonarWSClientFacade;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.rule.Rule;
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
import org.sonar.wsclient.user.User;

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

  @Override
  public Date getLastAnalysisDate(String resourceKey) {
    Resource remoteResource = find(ResourceQuery.createForMetrics(resourceKey));
    if (remoteResource != null) {
      return remoteResource.getDate();
    }
    return null;
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

  @Override
  public List<ISonarIssueWithPath> getUnresolvedRemoteIssuesRecursively(String resourceKey, IProgressMonitor monitor) {
    int maxPageSize = -1;
    List<ISonarIssueWithPath> result = new ArrayList<ISonarIssueWithPath>();
    int pageIndex = 1;
    Issues issues;
    do {
      issues = findIssues(IssueQuery.create().componentRoots(resourceKey).resolved(false).pageSize(maxPageSize).pageIndex(pageIndex));
      for (Issue issue : issues.list()) {
        Component comp = issues.component(issue);
        String path = comp != null ? comp.path() : null;
        String componentKey = comp != null ? comp.key() : null;
        result.add(new SonarRemoteIssue(issue, issues.rule(issue), issues.user(issue.assignee()), path, componentKey));
      }
    } while (pageIndex++ < issues.paging().pages() && !monitor.isCanceled());
    return result;
  }

  @Override
  public List<ISonarIssue> getUnresolvedRemoteIssues(String resourceKey, IProgressMonitor monitor) {
    Issues issues = findIssues(IssueQuery.create().components(resourceKey).resolved(false));
    List<ISonarIssue> result = new ArrayList<ISonarIssue>(issues.list().size());
    for (Issue issue : issues.list()) {
      result.add(new SonarRemoteIssue(issue, issues.rule(issue), issues.user(issue.assignee()), null, resourceKey));
    }
    return result;
  }

  private Issues findIssues(IssueQuery query) {
    try {
      return sonarClient.issueClient().find(query);
    } catch (ConnectionException e) {
      throw new org.sonar.ide.eclipse.wsclient.ConnectionException(e);
    } catch (Exception e) {
      throw new org.sonar.ide.eclipse.wsclient.SonarWSClientException("Error during issue query " + query.toString(), e);
    }
  }

  private static class SonarRemoteIssue implements ISonarIssueWithPath {

    private final Issue remoteIssue;
    private final Rule rule;
    private final User assignee;
    private final String path;
    private final String componentKey;

    public SonarRemoteIssue(final Issue remoteIssue, final Rule rule, @Nullable final User assignee, @Nullable final String path, String componentKey) {
      this.remoteIssue = remoteIssue;
      this.rule = rule;
      this.assignee = assignee;
      this.path = path;
      this.componentKey = componentKey;
    }

    @Override
    public String key() {
      return remoteIssue.key();
    }

    @Override
    public String resourceKey() {
      return componentKey;
    }

    @Override
    public String path() {
      return path;
    }

    @Override
    public boolean resolved() {
      return !Strings.isNullOrEmpty(remoteIssue.resolution());
    }

    @Override
    public Integer line() {
      return remoteIssue.line();
    }

    @Override
    public String severity() {
      return remoteIssue.severity();
    }

    @Override
    public String message() {
      return remoteIssue.message();
    }

    @Override
    public String ruleKey() {
      return rule.key();
    }

    @Override
    public String ruleName() {
      return rule.name();
    }

    @Override
    public String assigneeLogin() {
      return remoteIssue.assignee();
    }

    @Override
    public String assigneeName() {
      return assignee != null ? assignee.name() : null;
    }

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
