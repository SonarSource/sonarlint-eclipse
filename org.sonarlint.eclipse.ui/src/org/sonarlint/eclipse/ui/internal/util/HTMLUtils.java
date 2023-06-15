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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

/** Utility class used for parsing the HTML rule description into native elements */
public final class HTMLUtils {
  private static final Pattern preStartingPattern = Pattern.compile("<pre[^>]*>");
  private static final Pattern preEndingPattern = Pattern.compile("</pre>");
  private static final Pattern idPattern = Pattern.compile("data-diff-id=\"([^\"]*)\"");
  private static final Pattern typePattern = Pattern.compile("data-diff-type=\"([^\"]*)\"");

  private HTMLUtils() {
    // utility class
  }

  public static void parseIntoElements2(String html, Composite parent, String languageKey, boolean useEditorFontSize) {
    var elements = parse(html);

    for (int i = 0; i < elements.size(); i++) {
      var element = elements.get(i);
      var elementHTML = element.getHTML();

      if (element.getType() == ParsedHTMLElementType.OTHER) {
        // 1) other element
        var browser = new SonarLintWebView(parent, useEditorFontSize);
        browser.setLayoutData(createGridData());
        browser.setHtmlBody(elementHTML);
        continue;
      } else if (element.getType() == ParsedHTMLElementType.COMPLIANT_CODE_SNIPPET) {
        // 2) compliant code snippet element
        var other = elements.stream()
          .filter(e -> e.getId() == element.getId() && e.getType() == ParsedHTMLElementType.NON_COMPLIANT_CODE_SNIPPET)
          .collect(Collectors.toList());
        if (other.size() > 0) {
          var configuration = new CompareConfiguration();
          configuration.setLeftEditable(false);
          configuration.setLeftLabel("Noncompliant Code Example");
          configuration.setRightEditable(false);
          configuration.setRightLabel("Compliant Code Example");

          var mergeViewer = new TextMergeViewer(parent, configuration);
          mergeViewer.getControl().setLayoutData(createGridData());
          mergeViewer.setInput(new DiffNode(new TestNode(other.get(0).getHTML()), new TestNode(element.getHTML())));
          continue;
        }
      } else if (element.getType() == ParsedHTMLElementType.NON_COMPLIANT_CODE_SNIPPET) {
        // 3) non compliant code snippet element
        var other = elements.stream()
          .filter(e -> e.getId() == element.getId() && e.getType() == ParsedHTMLElementType.COMPLIANT_CODE_SNIPPET)
          .collect(Collectors.toList());
        if (other.size() > 0) {
          continue;
        }
      }

      // 4) normal code snippet
      createSourceViewer(StringUtils.xmlDecode(elementHTML), parent, languageKey);
    }
  }

  public static GridData createGridData() {
    var gridData = new GridData();
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    return gridData;
  }

  public static ArrayList<ParsedHTMLElement> parse(String html) {
    var elements = new ArrayList<ParsedHTMLElement>();

    var currentHTML = html;
    var matcherStart = preStartingPattern.matcher(currentHTML);
    var matcherEnd = preEndingPattern.matcher(currentHTML);

    while (matcherStart.find() && matcherEnd.find()) {
      var front = currentHTML.substring(0, matcherStart.start()).trim();
      if (!front.isEmpty() && !front.isBlank()) {
        elements.add(new ParsedHTMLElement(front, ParsedHTMLElementType.OTHER));
      }

      var middle = currentHTML.substring(matcherStart.end(), matcherEnd.start()).trim();
      if (!middle.isEmpty() && !middle.isBlank()) {
        var possibleId = idPattern.matcher(currentHTML.substring(matcherStart.start(), matcherStart.end()).trim());
        var id = possibleId.find() ? possibleId.group(1) : null;
        var possibleType = typePattern.matcher(currentHTML.substring(matcherStart.start(), matcherStart.end()).trim());
        var type = possibleType.find() ? possibleType.group(1) : null;

        var actualType = type != null
          ? (type.equalsIgnoreCase("compliant")
            ? ParsedHTMLElementType.COMPLIANT_CODE_SNIPPET
            : (type.equalsIgnoreCase("noncompliant")
              ? ParsedHTMLElementType.NON_COMPLIANT_CODE_SNIPPET
              : ParsedHTMLElementType.NORMAL_CODE_SNIPPET))
          : ParsedHTMLElementType.NORMAL_CODE_SNIPPET;

        elements.add(new ParsedHTMLElement(middle, actualType, id != null ? Integer.parseInt(id) : ParsedHTMLElement.INVALID));
      }

      currentHTML = currentHTML.substring(matcherEnd.end(), currentHTML.length()).trim();
      matcherStart = preStartingPattern.matcher(currentHTML);
      matcherEnd = preEndingPattern.matcher(currentHTML);
    }

    if (!currentHTML.isEmpty() && !currentHTML.isBlank()) {
      elements.add(new ParsedHTMLElement(currentHTML, ParsedHTMLElementType.OTHER));
    }

    return elements;
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
    var configurationProviders = SonarLintExtensionTracker.getInstance().getSyntaxHighlightingProvider();
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
