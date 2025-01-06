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

import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.sonarlint.eclipse.core.internal.backend.FileSystemSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessRpcClient;

/**
 *  For caching the information aggregated from the implementations of the extension point. Currently, only used for
 *  providing all files to SLCORE when the project is imported ({@link SonarLintEclipseHeadlessRpcClient#listFiles})
 *  and for the file system synchronization ({@link FileSystemSynchronizer#visitDeltaPostChange}).
 *
 *  But due to computation over the extension points is quite costly, having a cache in place for something that isn't
 *  changing too often is helpful. E.g. in case multiple changes are coming in at a time and the file system
 *  synchronization needs to happen one after another.
 */
public class IProjectScopeProviderCache extends AbstractConfigScopeIdCache<Set<IPath>> {
  public static final IProjectScopeProviderCache INSTANCE = new IProjectScopeProviderCache();

  /**
   *  Cache this information for 60 seconds as changes to the project "properties" (e.g. JDT, Maven, Gradle, ...) don't
   *  happen too often, maybe once every now and then! Therefore when calculated once we can re-use this information
   *  for the next 60 seconds and when it is requested every now and then, it is okey to re-calculate it!
   */
  @Override
  protected long getCacheDuration() {
    return 60_000;
  }
}
