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
package org.sonarlint.eclipse.core.internal.backend;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.jobs.AnalysisState;

/**
 * The analysis state will be updated and queried from two different places
 */
public class RunningAnalysesTracker {
  private static final RunningAnalysesTracker INSTANCE = new RunningAnalysesTracker();

  public static RunningAnalysesTracker get() {
    return INSTANCE;
  }

  private final Map<UUID, AnalysisState> analysisStateById = new ConcurrentHashMap<>();

  public void track(AnalysisState analysisState) {
    analysisStateById.put(analysisState.getId(), analysisState);
  }

  public void finish(AnalysisState analysisState) {
    analysisStateById.remove(analysisState.getId());
  }

  @Nullable
  public AnalysisState getById(UUID analysisId) {
    return analysisStateById.get(analysisId);
  }
}
