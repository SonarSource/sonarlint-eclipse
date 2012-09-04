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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.WSClientFactory;
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

  private TaskRepository repository;

  public SonarClient(TaskRepository repository) {
    this.repository = repository;
  }

  public Review getReview(long id, IProgressMonitor monitor) {
    Sonar sonar = create();
    Review review = sonar.find(new ReviewQuery().setId(id));
    if (review == null) {
      // Workaround for SONAR-2421, can be removed after upgrade of minimal Sonar version to 2.9
      review = sonar.find(new ReviewQuery().setId(id).setReviewType(TYPE_FALSE_POSITIVE));
    }
    return review;
  }

  /**
   * Visibility has been relaxed for test.
   */
  public ReviewQuery convertQuery(IRepositoryQuery query) {
    String currentUser = repository.getCredentials(AuthenticationType.REPOSITORY).getUserName();
    ReviewQuery result = new ReviewQuery();
    result.setProjectKeysOrIds(query.getAttribute(SonarQuery.PROJECT));
    result.setAuthorLoginsOrIds(SonarQuery.getReporter(query, currentUser));
    result.setAssigneeLoginsOrIds(SonarQuery.getAssignee(query, currentUser));
    result.setStatuses(SonarQuery.getStatuses(query));
    result.setSeverities(SonarQuery.getSeverities(query));
    return result;
  }

  public Collection<Review> getReviews(IRepositoryQuery query, IProgressMonitor monitor) {
    Sonar sonar = create();
    return sonar.findAll(convertQuery(query));
  }

  /**
   * @since Sonar 2.9
   */
  public void addComment(long id, String comment, IProgressMonitor monitor) {
    if (comment != null && comment.length() > 0) {
      Sonar sonar = create();
      sonar.update(ReviewUpdateQuery.addComment(id, comment));
    }
  }

  /**
   * @since Sonar 2.9
   */
  public void resolve(long id, String resolution, String comment, IProgressMonitor monitor) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.resolve(id, resolution).setComment(comment));
  }

  /**
   * @since Sonar 2.9
   */
  public void reassign(long id, String user, IProgressMonitor monitor) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.reassign(id, user));
  }

  /**
   * @since Sonar 2.9
   */
  public void reopen(long id, String comment, IProgressMonitor monitor) {
    Sonar sonar = create();
    sonar.update(ReviewUpdateQuery.reopen(id).setComment(comment));
  }

  /**
   * @since Sonar 2.9
   */
  public Review create(long violationId, String status, String resolution, String comment, String assignee, IProgressMonitor monitor) {
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

  private Sonar create() {
    return WSClientFactory.create(getSonarHost());
  }

  /**
   * Visibility has been relaxed for test.
   */
  public Host getSonarHost() {
    Host host = new Host(repository.getRepositoryUrl());
    AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
    if (credentials != null) {
      host.setUsername(credentials.getUserName());
      host.setPassword(credentials.getPassword());
    }
    return host;
  }
}
