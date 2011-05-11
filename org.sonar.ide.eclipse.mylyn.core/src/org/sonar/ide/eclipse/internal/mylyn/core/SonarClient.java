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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SonarClient {

  public Review getReview(long id, IProgressMonitor monitor) {
    Review review = new Review();
    review.id = id;
    review.createdAt = new Date(id);
    review.updatedAt = new Date(id);
    review.authorLogin = "admin";
    review.assigneeLogin = "admin";
    review.title = "Title";
    if (id == 1) {
      review.status = "closed";
      review.severity = "blocker";
    } else {
      review.status = "open";
      review.severity = "major";
    }
    review.comments = new ArrayList<Review.Comment>();
    Review.Comment comment = new Review.Comment();
    comment.authorLogin = "admin";
    comment.updatedAt = new Date(id);
    comment.text = "Text";
    review.comments.add(comment);
    return review;
  }

  public List<Review> getReviews(IProgressMonitor monitor) {
    return Arrays.asList(getReview(1, monitor), getReview(2, monitor));
  }

}
