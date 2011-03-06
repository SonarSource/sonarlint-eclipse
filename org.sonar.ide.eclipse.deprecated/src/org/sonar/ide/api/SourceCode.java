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
package org.sonar.ide.api;

import java.util.Set;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public interface SourceCode extends Comparable<SourceCode>, Measurable {

  String getKey();

  String getName();

  Set<SourceCode> getChildren();

  /**
   * @return content, which was analyzed by Sonar
   */
  String getRemoteContent();

  /**
   * HIGHLY EXPERIMENTAL!!!
   * 
   * @param content content of this resource
   * @return this (for method chaining)
   */
  SourceCode setLocalContent(String content);

}
