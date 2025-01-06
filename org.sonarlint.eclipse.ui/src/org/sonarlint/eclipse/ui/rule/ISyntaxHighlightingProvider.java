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
package org.sonarlint.eclipse.ui.rule;

import java.util.Optional;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;

/**
 *  The rule descriptions containing code snippet require the correct syntax highlighting of the language to display
 *  everything correctly and in the same manner as the UI theme. The configuration is provided by the specific Eclipse
 *  plug-in.
 *
 *  @since 7.12
 */
public interface ISyntaxHighlightingProvider {
  /**
   *  @return the SourceViewer configuration for syntax highlighting for the rule language, or null when none found
   */
  Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage);

  /**
   *  @return the SourceViewer and its document requiring a partitioner that is based on the plug-in implementing it
   */
  Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage);

  /**
   *  This is used to check whether a editor is provided by a specific plug-in (e.g. by JDT) and therefore linked to a
   *  specific programming language (e.g. Java). As to the Eclipse platform itself this is all the same shared
   *  interface this check must be done by the sub-plugins directly.
   *
   *  @since 10.6
   *
   *  @param editor used to determine the programming language based on the plug-in specific implementation
   *  @return the language or null if not applicable
   */
  @Nullable
  default SonarLintLanguage getEditorLanguage(IEditorPart editor) {
    return null;
  }

  /**
   *  This is used for displaying language (and therefore implementing plug-in) -specific difference viewers. The
   *  plug-in directly provides its implementation based on the parent element and the compare configuration that will
   *  be enhanced by the plug-in specific viewer.
   *  This is used for example by the "Open fix suggestion" feature in order to provide difference viewers based on the
   *  language the file has - enhancing the user experience.
   *
   *  @since 10.6
   *
   *  @param ruleLanguage to be used to check whether this plug-in can contribute a viewer for this language
   *  @param parent the UI parent element which will embed the viewer
   *  @param used for configuring the viewer by providing information on the left and right side, e.g. label
   *  @return the language/plug-in specific diff viewer or null if not applicable
   */
  @Nullable
  default TextMergeViewer getTextMergeViewer(String ruleLanguage, Composite parent, CompareConfiguration mp) {
    return null;
  }
}
