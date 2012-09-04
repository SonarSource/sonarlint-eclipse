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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WorkflowTest {

  @Test
  public void shouldProvideOperations() {
    assertThat(Workflow.OPERATIONS.length, is(7));
    for (Workflow.Operation operation : Workflow.OPERATIONS) {
      assertThat(Workflow.operationById(operation.getId()), is(operation));
    }
  }

  @Test
  public void testDefaultCreate() {
    Workflow.Operation operation = new Workflow.DefaultCreate();
    assertThat(operation.isDefault(), is(true));
  }

  @Test
  public void testCreateFixed() {
    Workflow.Operation operation = new Workflow.CreateFixed();
    assertThat(operation.isDefault(), is(false));
  }

  @Test
  public void testCreateFalsePositive() {
    Workflow.Operation operation = new Workflow.CreateFalsePositive();
    assertThat(operation.isDefault(), is(false));
  }

  @Test
  public void testDefault() {
    Workflow.Operation operation = new Workflow.Default();
    assertThat(operation.isDefault(), is(true));
  }

  @Test
  public void testResolveAsFixed() {
    Workflow.Operation operation = new Workflow.ResolveAsFixed();
    assertThat(operation.isDefault(), is(false));
  }

  @Test
  public void testResolveAsFalsePositive() {
    Workflow.Operation operation = new Workflow.ResolveAsFalsePositive();
    assertThat(operation.isDefault(), is(false));
  }

  @Test
  public void testReopen() {
    Workflow.Operation operation = new Workflow.Reopen();
    assertThat(operation.isDefault(), is(false));
  }

}
