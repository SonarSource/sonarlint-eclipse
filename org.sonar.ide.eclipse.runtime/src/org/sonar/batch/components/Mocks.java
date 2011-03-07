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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.MemoryOptimizer;

public class Mocks {

  private static final Answer<Object> UNSUPPORTED_OPERATION_ANSWER = new Answer<Object>() {
    public Object answer(InvocationOnMock invocation) throws Throwable {
      throw new UnsupportedOperationException(invocation.getMock().getClass().getSimpleName() + "#" + invocation.getMethod().getName());
    }
  };

  private static <T> T createSpecialMock(Class<T> classToMock) {
    T mock = mock(classToMock, UNSUPPORTED_OPERATION_ANSWER);
    doCallRealMethod().when(mock).toString();
    return mock;
  }

  public static ProjectTree createProjectTree(Project project) {
    ProjectTree projectTree = createSpecialMock(ProjectTree.class);
    try {
      doNothing().when(projectTree).start();
    } catch (Exception e) {
    }
    doReturn(project).when(projectTree).getRootProject();
    return projectTree;
  }

  public static DatabaseSession createDatabaseSession() {
    DatabaseSession session = createSpecialMock(DatabaseSession.class);
    doNothing().when(session).start();
    doNothing().when(session).stop();
    doNothing().when(session).commit();
    return session;
  }

  public static RuleFinder createRuleFinder() {
    RuleFinder ruleFinder = createSpecialMock(RuleFinder.class);
    doAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        String repositoryKey = (String) invocation.getArguments()[0];
        String key = (String) invocation.getArguments()[1];
        return Rule.create(repositoryKey, key, key);
      }
    }).when(ruleFinder).findByKey(anyString(), anyString());
    return ruleFinder;
  }

  public static MetricFinder createMetricFinder() {
    MetricFinder metricFinder = createSpecialMock(MetricFinder.class);
    doAnswer(new Answer<Metric>() {
      public Metric answer(InvocationOnMock invocation) throws Throwable {
        String key = (String) invocation.getArguments()[0];
        return new Metric(key);
      }
    }).when(metricFinder).findByKey(anyString());
    return metricFinder;
  }

  public static MemoryOptimizer createMemoryOptimizer() {
    return mock(MemoryOptimizer.class);
  }

}
