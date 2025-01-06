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

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.sonarlint.eclipse.core.internal.TriggerType;

public class AnalysisState {
  private final UUID id;
  private final List<URI> fileURIs;
  private final TriggerType triggerType;

  public AnalysisState(UUID analysisId, List<URI> fileURIs, TriggerType triggerType) {
    this.id = analysisId;
    this.fileURIs = fileURIs;
    this.triggerType = triggerType;
  }

  public List<URI> getFileURIs() {
    return fileURIs;
  }

  public UUID getId() {
    return id;
  }

  public TriggerType getTriggerType() {
    return triggerType;
  }
}
