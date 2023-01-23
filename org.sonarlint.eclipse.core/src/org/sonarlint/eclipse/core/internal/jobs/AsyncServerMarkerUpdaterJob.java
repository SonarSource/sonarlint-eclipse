/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class AsyncServerMarkerUpdaterJob extends AbstractSonarProjectJob {
  private final Map<ISonarLintIssuable, Collection<Trackable>> issuesPerResource;
  private final TriggerType triggerType;
  private final Map<ISonarLintFile, IDocument> docPerFile;

  public AsyncServerMarkerUpdaterJob(ISonarLintProject project, Map<ISonarLintIssuable, Collection<Trackable>> issuesPerResource, Map<ISonarLintFile, IDocument> docPerFile,
    TriggerType triggerType) {
    super("Update SonarLint markers based on server side issues", project);
    this.issuesPerResource = issuesPerResource;
    this.docPerFile = docPerFile;
    this.triggerType = triggerType;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    ResourcesPlugin.getWorkspace().run(this::updateMarkers, monitor);
    return Status.OK_STATUS;
  }

  private void updateMarkers(IProgressMonitor monitor) {
    for (var entry : issuesPerResource.entrySet()) {
      var issuable = entry.getKey();
      if (issuable instanceof ISonarLintFile) {
        var documentOrNull = docPerFile.get(issuable);
        final IDocument documentNotNull;
        if (documentOrNull == null) {
          documentNotNull = ((ISonarLintFile) issuable).getDocument();
        } else {
          documentNotNull = documentOrNull;
        }
        var markerRule = ResourcesPlugin.getWorkspace().getRuleFactory().markerRule(issuable.getResource());
        try {
          getJobManager().beginRule(markerRule, monitor);
          SonarLintMarkerUpdater.updateMarkersWithServerSideData(issuable, documentNotNull, entry.getValue(), triggerType);
        } finally {
          getJobManager().endRule(markerRule);
        }
      }
    }
  }
}
