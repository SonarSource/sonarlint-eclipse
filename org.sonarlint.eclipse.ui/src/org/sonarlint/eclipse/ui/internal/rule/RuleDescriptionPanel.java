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
package org.sonarlint.eclipse.ui.internal.rule;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.util.SonarLintWebView;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleSplitDescriptionDto;

/**
 *  Rule description (differs based on rule type / combination):
 *  ============================================================
 *
 *  -> monolithic description
 *  -> split description:
 *     -> introduction (optional)
 *     -> tab view
 *        -> tab description
 *  -> split description:
 *     -> introduction (optional)
 *     -> tab view
 *        -> context view
 *           -> context description
 */
public class RuleDescriptionPanel extends Composite {
  @Nullable
  private SonarLintWebView monolithicDescriptionOrIntro;
  @Nullable
  private TabFolder tabFolder;
  private final boolean useEditorFontSize;

  public RuleDescriptionPanel(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout());

    this.useEditorFontSize = useEditorFontSize;
  }

  public void updateRule(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description) {
    if (description.isLeft()) {
      // monolithic description
      var monoDescription = description.getLeft();
      monolithicDescriptionOrIntro = new SonarLintWebView(this, useEditorFontSize);
      monolithicDescriptionOrIntro.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      monolithicDescriptionOrIntro.setHtmlBody(monoDescription.getHtmlContent());
    } else {
      // split description
      var ruleDescription = description.getRight();

      var intro = ruleDescription.getIntroductionHtmlContent();
      if (StringUtils.isNotBlank(intro)) {
        // introduction (optional)
        monolithicDescriptionOrIntro = new SonarLintWebView(this, useEditorFontSize);
        monolithicDescriptionOrIntro.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        monolithicDescriptionOrIntro.setHtmlBody(intro);
      }

      // tab view
      tabFolder = new TabFolder(this, SWT.NONE);
      tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      for (var tab : ruleDescription.getTabs()) {
        var tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(tab.getTitle());

        var content = tab.getContent();
        if (content.isLeft()) {
          // tab description
          var nonContextualDescription = new SonarLintWebView(tabFolder, useEditorFontSize);
          nonContextualDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
          nonContextualDescription.setHtmlBody(content.getLeft().getHtmlContent());
          tabItem.setControl(nonContextualDescription);
        } else {
          // context view
          var contextualDescription = content.getRight();

          var contextualTabFolder = new TabFolder(tabFolder, SWT.NONE);
          for (var contextualTab : contextualDescription.getContextualSections()) {
            // context description
            var contextualTabItem = new TabItem(contextualTabFolder, SWT.NONE);
            contextualTabItem.setText(contextualTab.getContextKey());

            var contextualTabTitle = new StyledText(contextualTabFolder, SWT.NONE);
            contextualTabTitle.setText(contextualTab.getDisplayName());
            contextualTabItem.setControl(contextualTabTitle);

            var contextualDescriptionHTML = new SonarLintWebView(contextualTabFolder, useEditorFontSize);
            contextualDescriptionHTML.setHtmlBody(contextualTab.getHtmlContent());
            contextualTabItem.setControl(contextualDescriptionHTML);
          }
          tabItem.setControl(contextualTabFolder);
          contextualTabFolder.requestLayout();
        }
      }
      tabFolder.requestLayout();
    }
    requestLayout();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (monolithicDescriptionOrIntro != null && !monolithicDescriptionOrIntro.isDisposed()) {
      monolithicDescriptionOrIntro.dispose();
    }
    if (tabFolder != null && !tabFolder.isDisposed()) {
      tabFolder.dispose();
    }
  }
}
