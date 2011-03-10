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

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

/**
 * @TODO Godin: I suppose that this class should be in Sonar Core
 * @see RemoteProfileLoader
 * @see ProjectProfileLoader
 */
public class EmbedderProfileProvider extends ProviderAdapter {

  private RulesProfile profile;

  public RulesProfile provide(ProjectProfileLoader provider, Project project) {
    if (profile == null) {
      profile = provider.load(project);
    }
    return profile;
  }

}
