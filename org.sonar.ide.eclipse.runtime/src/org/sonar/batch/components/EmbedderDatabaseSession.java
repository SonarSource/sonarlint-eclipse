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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.sonar.api.database.DatabaseSession;

public class EmbedderDatabaseSession extends DatabaseSession {

  @Override
  public void commit() {
  }

  @Override
  public boolean contains(Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query createQuery(String hql) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getEntity(Class<T> entityClass, Object id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityManager getEntityManager() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<T> getResults(Class<T> entityClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<T> getResults(Class<T> entityClass, Object... criterias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getSingleResult(Query query, T defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getSingleResult(Class<T> entityClass, Object... criterias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object merge(Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T reattach(Class<T> entityClass, Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeWithoutFlush(Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rollback() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T save(T entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void save(Object... entities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object saveWithoutFlush(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

}
