/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;

public class AsyncServerMarkerUpdaterJob extends AbstractSonarProjectJob {
  private final Map<IResource, Collection<Trackable>> issuesPerResource;

  public AsyncServerMarkerUpdaterJob(SonarLintProject project, Map<IResource, Collection<Trackable>> issuesPerResource) {
    super("Update SonarLint markers based on server side issues", project);
    this.issuesPerResource = issuesPerResource;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) {
    for (Map.Entry<IResource, Collection<Trackable>> entry : issuesPerResource.entrySet()) {
      SonarLintMarkerUpdater.updateMarkersWithServerSideData(entry.getKey(), entry.getValue());
    }
    return Status.OK_STATUS;
  }
}
