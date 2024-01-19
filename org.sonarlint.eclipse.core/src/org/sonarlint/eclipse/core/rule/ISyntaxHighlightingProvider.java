/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.rule;

import java.util.Optional;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

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
   *  @return the SourceViewer and its document requiring 
   */
  Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage);
}
