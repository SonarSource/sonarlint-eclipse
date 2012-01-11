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
package org.sonar.ide.wsclient;

import org.sonar.ide.api.SourceCode;
import org.sonar.ide.api.SourceCodeDiffEngine;
import org.sonar.ide.api.SourceCodeSearchEngine;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

import java.util.Collection;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public class RemoteSonar implements SourceCodeSearchEngine {

  private RemoteSonarIndex index;

  public RemoteSonar(Host host) {
    this(host, new SimpleSourceCodeDiffEngine());
  }

  public RemoteSonar(Host host, SourceCodeDiffEngine diffEngine) {
    index = new RemoteSonarIndex(host, diffEngine);
  }

  public SourceCode search(String key) {
    return index.search(key);
  }

  public Collection<SourceCode> getProjects() {
    return index.getProjects();
  }

  public Sonar getSonar() {
    return index.getSonar();
  }

}
