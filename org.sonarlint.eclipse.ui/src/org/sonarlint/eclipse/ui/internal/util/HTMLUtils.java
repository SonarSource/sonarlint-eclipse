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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.regex.Pattern;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.extension.SonarLintUiExtensionTracker;

/** Utility class used for parsing the HTML rule description into native elements */
public final class HTMLUtils {
  private static final Pattern preStartingPattern = Pattern.compile("<pre[^>]*>");
  private static final Pattern preEndingPattern = Pattern.compile("</pre>");

  private HTMLUtils() {
    // utility class
  }

  /**
   *  Parse HTML into native elements and only fallback on {@link SonarLintWebView} for non-parseable elements
   *
   *  @param html to be parsed
   *  @param parent to add the elements to
   *  @param languageKey required for code snippet (for syntax highlighting)
   *  @param useEditorFontSize for SonarLintWebView to use the same font size as other elements
   */
  public static void parseIntoElements(String html, Composite parent, String languageKey, boolean useEditorFontSize) {
    var currentHTML = html;
    var matcherStart = preStartingPattern.matcher(currentHTML);
    var matcherEnd = preEndingPattern.matcher(currentHTML);

    while (matcherStart.find() && matcherEnd.find()) {
      var front = currentHTML.substring(0, matcherStart.start()).trim();
      if (!front.isEmpty() && !front.isBlank()) {
        var frontFragment = new SonarLintWebView(parent, useEditorFontSize);
        var gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        frontFragment.setLayoutData(gridData);

        frontFragment.setHtmlBody(front);
      }

      var middle = currentHTML.substring(matcherStart.end(), matcherEnd.start()).trim();
      if (!middle.isEmpty() && !middle.isBlank()) {
        createSourceViewer(StringUtils.xmlDecode(middle), parent, languageKey);
      }

      currentHTML = currentHTML.substring(matcherEnd.end(), currentHTML.length()).trim();
      matcherStart = preStartingPattern.matcher(currentHTML);
      matcherEnd = preEndingPattern.matcher(currentHTML);
    }

    if (!currentHTML.isEmpty() && !currentHTML.isBlank()) {
      var endFragment = new SonarLintWebView(parent, useEditorFontSize);
      var gridData = new GridData();
      gridData.horizontalAlignment = SWT.FILL;
      gridData.grabExcessHorizontalSpace = true;
      endFragment.setLayoutData(gridData);

      endFragment.setHtmlBody(currentHTML);
    }
  }

  private static void createSourceViewer(String html, Composite parent, String languageKey) {
    // Configure the syntax highlighting based on the rule language key and if a configuration and document partitioner
    // is provided by any plug-in via the extension mechanism.
    // INFO: Configuration must extend of org.eclipse.jface.text.source.SourceViewerConfiguration
    // INFO: Document partitioner must implement org.eclipse.jface.text.IDocumentPartitioner
    var configurationProviders = SonarLintUiExtensionTracker.getInstance().getSyntaxHighlightingProvider();
    SourceViewerConfiguration sourceViewerConfigurationNullable = null;
    for (var configurationProvider : configurationProviders) {
      var sourceViewerConfigurationOptional = configurationProvider.sourceViewerConfiguration(languageKey);
      if (sourceViewerConfigurationOptional.isPresent()) {
        sourceViewerConfigurationNullable = sourceViewerConfigurationOptional.get();
        break;
      }
    }

    IDocumentPartitioner documentPartitionerNullable = null;
    for (var configurationProvider : configurationProviders) {
      var documentPartitionerOptional = configurationProvider.documentPartitioner(languageKey);
      if (documentPartitionerOptional.isPresent()) {
        documentPartitionerNullable = documentPartitionerOptional.get();
        break;
      }
    }

    var snippetElement = new SourceViewer(parent, null, SWT.BORDER | SWT.H_SCROLL);
    var gridData = new GridData();
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalIndent = 10;
    snippetElement.getTextWidget().setLayoutData(gridData);

    var content = new Document(html);
    if (sourceViewerConfigurationNullable != null && documentPartitionerNullable != null) {
      content.setDocumentPartitioner(
        sourceViewerConfigurationNullable.getConfiguredDocumentPartitioning(snippetElement),
        documentPartitionerNullable);
      content.setDocumentPartitioner(documentPartitionerNullable);
      documentPartitionerNullable.connect(content);
    }

    if (sourceViewerConfigurationNullable != null) {
      snippetElement.configure(sourceViewerConfigurationNullable);
    }

    snippetElement.setDocument(content);
    snippetElement.setEditable(false);
  }
}
