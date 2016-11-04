/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.tracking;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;

/**
 * Manage a mapping of local module keys to their physical filesystem paths.
 */
public class ModulePathManager {

  private final Map<String, String> modulePaths = new ConcurrentHashMap<>();

  public void setModulePath(String localModuleKey, String path) {
    modulePaths.put(localModuleKey, path);
  }

  @CheckForNull
  public String getModulePath(String localModuleKey) {
    return modulePaths.get(localModuleKey);
  }

  public String getFilePath(String localModuleKey, String relativePath) {
    return new File(modulePaths.get(localModuleKey), relativePath).getAbsolutePath();
  }

}
