/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.jobs;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.coverage.CoverageLine;

/**
 * This class load code coverage in background.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-60
 * 
 * @author Jérémie Lagarde
 */
public class RefreshCoverageJob extends Job {

  protected AbstractDecoratedTextEditor targetEditor;

  public RefreshCoverageJob(final AbstractDecoratedTextEditor targetEditor) {
    super("RefreshCoverageJob");
    this.targetEditor = targetEditor;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {

    if (targetEditor != null) {
      final IDocument doc = getDocument();
      final IAnnotationModel model = targetEditor.getDocumentProvider().getAnnotationModel(targetEditor.getEditorInput());
      final IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(resource);
      final Collection<CoverageLine> coverageLines = EclipseSonar.getInstance(resource.getProject()).search(resourceKey).getCoverage()
      .getCoverageLines();

      for (final CoverageLine coverage : coverageLines) {
        final String hits = coverage.getHits();
        final String branchHits = coverage.getBranchHits();
        final boolean hasLineCoverage = (null != hits);
        final boolean hasBranchCoverage = (null != branchHits);
        final boolean lineIsCovered = (hasLineCoverage && Integer.parseInt(hits) > 0);
        final boolean branchIsCovered = (hasBranchCoverage && "100%".equals(branchHits));
        try {
          final IRegion region = doc.getLineInformation(coverage.getLine() - 1);
          final Position position = new Position(region.getOffset(), region.getLength());
          final Position positionFull = new Position(region.getOffset(), 1);

          if (lineIsCovered) {
            if (branchIsCovered) {
              model
              .addAnnotation(new Annotation("org.sonar.ide.eclipse.fullCoverageAnnotationType", false, getMessage(coverage)),
                  positionFull);
            } else if (hasBranchCoverage) {
              model.addAnnotation(new Annotation("org.sonar.ide.eclipse.partialCoverageAnnotationType", false, getMessage(coverage)),
                  position);
            } else {
              model
              .addAnnotation(new Annotation("org.sonar.ide.eclipse.fullCoverageAnnotationType", false, getMessage(coverage)),
                  positionFull);
            }
          } else if (hasLineCoverage) {
            model.addAnnotation(new Annotation("org.sonar.ide.eclipse.noCoverageAnnotationType", false, getMessage(coverage)), position);
          }
        } catch (final Exception ex) {
          SonarPlugin.getDefault().displayError(IStatus.WARNING, "Error in RefreshCoverageJob.", ex, true); //$NON-NLS-1$
        }
      }

    }
    return Status.OK_STATUS;
  }

  protected final IDocument getDocument() {
    final IDocumentProvider provider = targetEditor.getDocumentProvider();
    return provider.getDocument(targetEditor.getEditorInput());
  }

  protected String getMessage(final CoverageLine coverage) {
    if ("0".equals(coverage.getHits())) {
      String hits = StringUtils.leftPad(coverage.getHits(), 2);
      String branch = StringUtils.leftPad((coverage.getBranchHits() != null ? coverage.getBranchHits() : "    "), 4);
      return hits + " " + branch;
    } else {
      String hits = StringUtils.leftPad(coverage.getHits(), 2);
      String branch = StringUtils.leftPad((coverage.getBranchHits() != null ? coverage.getBranchHits() : "    "), 4);
      return hits + " " + branch;
    }
  }

}
