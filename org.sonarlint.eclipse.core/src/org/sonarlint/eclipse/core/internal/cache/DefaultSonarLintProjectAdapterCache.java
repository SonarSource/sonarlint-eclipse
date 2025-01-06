/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.cache;

import java.util.List;
import org.sonarlint.eclipse.core.internal.backend.FileSystemSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessRpcClient;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 *  For caching the files in a project. Currently, only used for providing all files to SLCORE when the project is
 *  imported ({@link SonarLintEclipseHeadlessRpcClient#listFiles}), internally in this logic to get all shared
 *  Connected Mode configuration files in hierarchies ({@link FileSystemSynchronizer#getSonarLintJsonFiles}) and when
 *  aggregating all files to be analyzed on a manual selection (see `SelectionUtils.collectFiles` in the UI plug-in).
 */
public class DefaultSonarLintProjectAdapterCache extends AbstractConfigScopeIdCache<List<ISonarLintFile>> {
  public static final DefaultSonarLintProjectAdapterCache INSTANCE = new DefaultSonarLintProjectAdapterCache();

  /**
   *  Cache this information for 30 seconds as adding / removing files of a project can happen quite often. For changes
   *  to the project properties this might not happen too often ({@link IProjectScopeProviderCache}), another cache is
   *  in place and will be re-used when computing new entries for this cache.
   */
  @Override
  protected long getCacheDuration() {
    return 30_000;
  }
}
