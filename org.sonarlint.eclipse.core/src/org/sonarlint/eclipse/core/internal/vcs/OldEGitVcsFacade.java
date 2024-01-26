/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.vcs;

import java.util.Optional;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * Uses internal EGit
 * @deprecated Remove when we want to stop support of Eclipse with EGit < 5.12 (= Eclipse 2021-06)
 */
@Deprecated(forRemoval = true)
public class OldEGitVcsFacade extends AbstractEGitVcsFacade {

  private static final SonarLintLogger LOG = SonarLintLogger.get();

  @Override
  public boolean isIgnored(ISonarLintFile file) {
    try {
      Class resourceStateFactoryClass = Class.forName("org.eclipse.egit.ui.internal.resources.ResourceStateFactory");
      var getInstance = resourceStateFactoryClass.getMethod("getInstance");
      var resourceStateFactory = getInstance.invoke(null);
      var get = resourceStateFactoryClass.getMethod("get", IResource.class);
      var resourceState = get.invoke(resourceStateFactory, file.getResource());
      Class resourceStateClass = Class.forName("org.eclipse.egit.ui.internal.resources.ResourceState");
      var isIgnored = resourceStateClass.getMethod("isIgnored");
      return (boolean) isIgnored.invoke(resourceState);
    } catch (Exception e) {
      LOG.debug("Unable to call eGit", e);
      return false;
    }
  }

  @Override
  Optional<Repository> getRepo(IResource resource) {
    var mapping = RepositoryMapping.getMapping(resource);
    return Optional.ofNullable(mapping).map(RepositoryMapping::getRepository);
  }

}
