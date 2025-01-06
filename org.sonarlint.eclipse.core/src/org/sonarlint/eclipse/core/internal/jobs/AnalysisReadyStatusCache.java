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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Because we have to await SonarLint out of Process to get ready (e.g. on startup or when a new connection/binding
 *  was established for projects, changes to configurations), we have to keep the information, that might change, in
 *  sync as different parts of SonarLint rely on that information (e.g. analysis itself, Open in IDE).
 *
 *  The cache can be accessed from different threads and Eclipse platform jobs (basically fancy threads) and therefore
 *  must be thread safe!
 */
public class AnalysisReadyStatusCache {
  private static final Map<String, Boolean> analysisReadyByConfigurationScopeId = new ConcurrentHashMap<>();

  private AnalysisReadyStatusCache() {
    // utility class
  }

  public static Map<String, Boolean> getCache() {
    return analysisReadyByConfigurationScopeId;
  }

  public static void changeAnalysisReadiness(String configurationScopeId, boolean readiness) {
    analysisReadyByConfigurationScopeId.put(configurationScopeId, readiness);
  }

  public static boolean getAnalysisReadiness(String configurationScopeId) {
    return analysisReadyByConfigurationScopeId.getOrDefault(configurationScopeId, false);
  }
}
