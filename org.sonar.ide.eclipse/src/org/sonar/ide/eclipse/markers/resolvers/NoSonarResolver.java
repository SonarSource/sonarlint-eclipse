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

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

/**
 * @author Jérémie Lagarde
 */
public class NoSonarResolver implements ISonarResolver {

  public boolean canResolve(final IMarker marker) {
    return true;
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getLabel() {
    return "Add //NOSONAR tag.";
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
      if (lineSource.contains("//NOSONAR")) {
        return false;
      }
      addNoSonarComments(cu, endOfLine, new NullProgressMonitor());
      return true;
    } catch (final JavaModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final BadLocationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }

  private void addNoSonarComments(final ICompilationUnit cu, final int position, final IProgressMonitor monitor) throws CoreException {

    final ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
    final IPath path = cu.getPath();

    manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
    try {
      final IDocument document = manager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
      final MultiTextEdit edit = new MultiTextEdit();

      edit.addChild(new InsertEdit(position, " //NOSONAR"));

      monitor.worked(1);
      edit.apply(document);
    } catch (final BadLocationException e) {
      e.printStackTrace();
    } finally {
      manager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
    }
  }
}
