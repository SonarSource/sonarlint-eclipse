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
package org.sonarlint.eclipse.cdt.internal;

import org.eclipse.cdt.internal.ui.compare.CMergeViewer;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.text.doctools.DocCommentOwnerManager;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.CSourceViewerConfiguration;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

public class CdtUiUtils {

  private CdtUiUtils() {

  }

  public static SourceViewerConfiguration sourceViewerConfiguration() {
    var tools = CUIPlugin.getDefault().getTextTools();
    return new CSourceViewerConfiguration(tools.getColorManager(),
      CUIPlugin.getDefault().getCorePreferenceStore(), null, tools.getDocumentPartitioning());
  }

  public static IDocumentPartitioner documentPartitioner() {
    // use workspace default for highlighting doc comments in compare viewer
    var owner = DocCommentOwnerManager.getInstance().getWorkspaceCommentOwner();
    return CUIPlugin.getDefault().getTextTools().createDocumentPartitioner(owner);
  }

  public static boolean isCEditor(IEditorPart editor) {
    return editor instanceof CEditor;
  }

  public static TextMergeViewer getTextMergeViewer(Composite parent, CompareConfiguration mp) {
    return new CMergeViewer(parent, 0, mp);
  }
}
