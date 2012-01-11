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
package org.sonar.ide.api;

import java.util.List;

import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.Violation;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public interface Measurable {

  List<IMeasure> getMeasures();

  /*
   * TODO Godin:
   * I'm not sure that following methods should be here.
   * Actually those methods work only for files.
   */

  List<Violation> getViolations();

  List<Violation> getViolations2();

  List<Rule> getRules();

}
