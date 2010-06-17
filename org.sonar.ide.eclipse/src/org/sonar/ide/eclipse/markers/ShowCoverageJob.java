/*
 * Copyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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
package org.sonar.ide.eclipse.markers;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.sonar.ide.eclipse.EclipseSonar;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.coverage.CoverageLine;
import org.sonar.wsclient.Sonar;

/**
 * @author Jérémie Lagarde
 */
public class ShowCoverageJob extends Job {


  protected AbstractDecoratedTextEditor targetEditor;

  public ShowCoverageJob(final AbstractDecoratedTextEditor targetEditor) {
    super("ShowCoverageJob");
    this.targetEditor = targetEditor;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {

    if (targetEditor != null) {
      final IDocument doc = getDocument();
      final IAnnotationModel model = targetEditor.getDocumentProvider().getAnnotationModel(targetEditor.getEditorInput());
      final IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
      final Sonar sonar = getSonar(resource.getProject());
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(resource);
      final Collection<CoverageLine> coverageLines = new EclipseSonar(sonar).search(resourceKey).getCoverage().getCoverageLines();

      for (final CoverageLine coverage : coverageLines) {
        final String hits = coverage.getHits();
        final String branchHits = coverage.getBranchHits();
        final boolean hasLineCoverage = (null != hits);
        final boolean hasBranchCoverage = (null != branchHits);
        final boolean lineIsCovered = (hasLineCoverage && Integer.parseInt(hits) > 0);
        final boolean branchIsCovered = (hasBranchCoverage && "100%".equals(branchHits));
        try {
          final Position position = new Position(doc.getLineOffset(coverage.getLine()), doc.getLineOffset(coverage.getLine() + 1));

          if (lineIsCovered) {
            if (branchIsCovered) {
              model
              .addAnnotation(new Annotation("org.sonar.ide.eclipse.fullCoverageAnnotationType", false, getMessage(coverage)), position);
            } else if (hasBranchCoverage) {
              model.addAnnotation(new Annotation("org.sonar.ide.eclipse.partialCoverageAnnotationType", false, getMessage(coverage)),
                  position);
            } else {
              model
              .addAnnotation(new Annotation("org.sonar.ide.eclipse.fullCoverageAnnotationType", false, getMessage(coverage)), position);
            }
          } else if (hasLineCoverage) {
            model.addAnnotation(new Annotation("org.sonar.ide.eclipse.noCoverageAnnotationType", false, getMessage(coverage)), position);
          }
        } catch (final Exception ex) {
          // TODO : best exception management.
          ex.printStackTrace();
        }
      }

    }
    return Status.OK_STATUS;
  }

  protected final IDocument getDocument() {
    final IDocumentProvider provider = targetEditor.getDocumentProvider();
    return provider.getDocument(targetEditor.getEditorInput());
  }

  protected Sonar getSonar(final IProject project) {
    final ProjectProperties properties = ProjectProperties.getInstance(project);
    final Sonar sonar = SonarPlugin.getServerManager().getSonar(properties.getUrl());
    return sonar;
  }

  protected String getMessage(final CoverageLine coverage) {
    // TODO jérémie : improve message and so on
    return "Coverage " + coverage.getHits() + ":" + coverage.getBranchHits();
  }

}
