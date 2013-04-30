/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.sonar.ide.eclipse.wsclient.SonarConnectionTester;
import org.sonar.ide.eclipse.wsclient.SonarConnectionTester.ConnectionTestResult;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Review;
import org.sonar.wsclient.services.ReviewCreateQuery;
import org.sonar.wsclient.services.ReviewQuery;
import org.sonar.wsclient.services.ReviewUpdateQuery;
import org.sonar.wsclient.services.ServerQuery;

import java.util.Collection;

public class SonarClient {

  public static final String TYPE_VIOLATION = "FALSE_POSITIVE"; //$NON-NLS-1$
  public static final String TYPE_FALSE_POSITIVE = "FALSE_POSITIVE"; //$NON-NLS-1$

  public static final String STATUS_OPEN = "OPEN"; //$NON-NLS-1$
  public static final String STATUS_CLOSED = "CLOSED"; //$NON-NLS-1$

  /**
   * @since Sonar 2.9
   */
  public static final String STATUS_RESOLVED = "RESOLVED"; //$NON-NLS-1$

  /**
   * @since Sonar 2.9
   */
  public static final String STATUS_REOPENED = "REOPENED"; //$NON-NLS-1$

  /**
   * @since Sonar 2.9
   */
  public static final String RESOLUTION_FIXED = "FIXED"; //$NON-NLS-1$

  /**
   * @since Sonar 2.9
   */
  public static final String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE"; //$NON-NLS-1$

  public static final String PRIORITY_BLOCKER = "BLOCKER"; //$NON-NLS-1$
  public static final String PRIORITY_CRITICAL = "CRITICAL"; //$NON-NLS-1$
  public static final String PRIORITY_MAJOR = "MAJOR"; //$NON-NLS-1$
  public static final String PRIORITY_MINOR = "MINOR"; //$NON-NLS-1$
  public static final String PRIORITY_INFO = "INFO"; //$NON-NLS-1$

  private final TaskRepository repository;

  public SonarClient(TaskRepository repository) {
    this.repository = repository;
  }

  public Review getReview(long id) {
    Sonar sonar = create();
    return sonar.find(new ReviewQuery().setId(id));
  }

  /**
   * Visibility has been relaxed for test.
   */
  public ReviewQuery convertQuery(IRepositoryQuery query) {
    String currentUser = repository.getUserName();
    ReviewQuery result = new ReviewQuery();
    result.setProjectKeysOrIds(query.getAttribute(SonarQuery.PROJECT));
    result.setAuthorLogins(SonarQuery.getReporter(query, currentUser));
    result.setAssigneeLogins(SonarQuery.getAssignee(query, currentUser));
    result.setStatuses(SonarQuery.getStatuses(query));
    result.setSeverities(SonarQuery.getSeverities(query));
    return result;
  }

  public Collection<Review> getReviews(IRepositoryQuery query) {
    Sonar sonar = create();
    return sonar.findAll(convertQuery(query));
  }

  /**
   * @since Sonar 2.9
   */
  public void addComment(long id, String comment) {
    if ((comment != null) && (comment.length() > 0)) {
      Sonar sonar = create();
      sonar.update(ReviewUpdateQuery.addComment(id, comment));
    }
  }

  /**
   * @since Sonar 2.9
   */
  public void resolve(long id, String resolution, String comment) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.resolve(id, resolution).setComment(comment));
  }

  /**
   * @since Sonar 2.9
   */
  public void reassign(long id, String user) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.reassign(id, user));
  }

  /**
   * @since Sonar 2.9
   */
  public void reopen(long id, String comment) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.reopen(id).setComment(comment));
  }

  /**
   * @since Sonar 2.9
   */
  public Review create(long violationId, String status, String resolution, String comment, String assignee) {
    ReviewCreateQuery query = new ReviewCreateQuery()
        .setViolationId(violationId)
        .setStatus(status)
        .setResolution(resolution)
        .setComment(comment)
        .setAssignee(assignee);
    return create().create(query);
  }

  public String getServerVersion() {
    return create().find(new ServerQuery()).getVersion();
  }

  public ConnectionTestResult validateCredentials() {
    AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
    String userName = repository.getUserName();
    String password = credentials.getPassword();
    return new SonarConnectionTester().testSonar(repository.getRepositoryUrl(), userName, password);
  }

  private Sonar create() {
    return WSClientFactory.create(getSonarHost());
  }

  /**
   * Visibility has been relaxed for test.
   */
  public Host getSonarHost() {
    Host host = new Host(repository.getRepositoryUrl());
    host.setUsername(repository.getUserName());
    AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
    if (credentials != null) {
      host.setPassword(credentials.getPassword());
    }
    return host;
  }
}
