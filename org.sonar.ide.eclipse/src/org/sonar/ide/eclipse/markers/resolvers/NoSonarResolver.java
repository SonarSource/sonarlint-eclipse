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

package org.sonar.ide.eclipse.markers.resolvers;

import java.text.MessageFormat;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class NoSonarResolver implements ISonarResolver {

  private final String NOSONAR_TAG = "//NOSONAR"; //$NON-NLS-1$

  private String       label;
  private String       description;

  public boolean canResolve(final IMarker marker) {
    try {
      if (SonarPlugin.MARKER_VIOLATION_ID.equals(marker.getType())) {
        final Object ruleName = marker.getAttribute("rulename"); //$NON-NLS-1$
        label = MessageFormat.format(Messages.getString("resolver.nosonartag.label"), ruleName); //$NON-NLS-1$
        description = MessageFormat.format(Messages.getString("resolver.nosonartag.description"), ruleName); //$NON-NLS-1$
        return true;
      }
    } catch (final CoreException e) {
      return false;
    }
    return false;
  }

  public String getDescription() {
    return description;
  }

  public String getLabel() {
    return label;
  }

  public boolean resolve(final IMarker marker, final ICompilationUnit cu) {
    final int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
    if (line == -1 || cu == null) {
      return false;
    }
    try {
      final String source = cu.getSource();
      final Document document = new Document(source);
      final IRegion region = document.getLineInformation(line - 1);
      final int endOfLine = region.getOffset() + region.getLength();
      final String lineSource = source.substring(region.getOffset(), endOfLine);
      if (lineSource.contains(NOSONAR_TAG)) {
        return false;
      }
      addNoSonarComments(cu, endOfLine, new NullProgressMonitor());
      return true;
    } catch (final Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.WARNING, "Error in NOSONAR tag resolver.", e, true); //$NON-NLS-1$
    }
    return true;
  }

  private void addNoSonarComments(final ICompilationUnit cu, final int position, final IProgressMonitor monitor) throws Exception {
    final ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
    final IPath path = cu.getPath();

    manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
    try {
      final IDocument document = manager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
      final MultiTextEdit edit = new MultiTextEdit();

      edit.addChild(new InsertEdit(position, " " + NOSONAR_TAG)); //$NON-NLS-1$

      monitor.worked(1);
      edit.apply(document);
    } finally {
      manager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
    }
  }
}
