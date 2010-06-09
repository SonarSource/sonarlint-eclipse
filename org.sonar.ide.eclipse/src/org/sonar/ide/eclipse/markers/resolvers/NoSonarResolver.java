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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

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

    // creation of DOM/AST from a ICompilationUnit
    final ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    final CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

    // start record of the modifications
    astRoot.recordModifications();
    try {
      final String source = cu.getSource();
      final Document document = new Document(source);
      final IRegion region = document.getLineInformation(line - 1);
      final int endOfLine = region.getOffset() + region.getLength();
      document.replace(endOfLine, 0, " //NOSONAR");

      // computation of the text edits
      final TextEdit edits = astRoot.rewrite(document, cu.getJavaProject().getOptions(true));

      // computation of the new source code
      edits.apply(document);
      final String newSource = document.get();

      // update of the compilation unit
      cu.getBuffer().setContents(newSource);
    } catch (final JavaModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final MalformedTreeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final BadLocationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }

}
