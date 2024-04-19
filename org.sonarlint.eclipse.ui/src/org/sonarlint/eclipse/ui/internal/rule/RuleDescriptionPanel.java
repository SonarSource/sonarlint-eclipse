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
package org.sonarlint.eclipse.ui.internal.rule;

import java.util.Objects;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.util.HTMLUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleSplitDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;

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
  private final String languageKey;
  private final boolean useEditorFontSize;

  public RuleDescriptionPanel(Composite parent, String languageKey, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(1, false));

    this.languageKey = languageKey;
    this.useEditorFontSize = useEditorFontSize;
  }

  public void updateRule(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description) {
    if (description.isLeft()) {
      updateMonolithicRule(description.getLeft());
    } else {
      updateSplitRule(description.getRight());
    }
    requestLayout();
  }

  public void updateMonolithicRule(RuleMonolithicDescriptionDto description) {
    HTMLUtils.parseIntoElements(description.getHtmlContent(), this, languageKey, this.useEditorFontSize);
  }

  public void updateSplitRule(RuleSplitDescriptionDto description) {
    var intro = description.getIntroductionHtmlContent();
    if (StringUtils.isNotBlank(intro)) {
      // introduction (optional)
      HTMLUtils.parseIntoElements(intro, this, languageKey, this.useEditorFontSize);
    }

    // tab view
    var tabFolder = new TabFolder(this, SWT.NONE);
    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    for (var tab : description.getTabs()) {
      var tabItem = new TabItem(tabFolder, SWT.NONE);
      tabItem.setText(tab.getTitle());
      var tabContent = new Composite(tabFolder, SWT.NONE);
      tabContent.setLayout(new GridLayout(1, false));

      var content = tab.getContent();
      if (content.isLeft()) {
        // tab description
        HTMLUtils.parseIntoElements(content.getLeft().getHtmlContent(), tabContent, languageKey,
          this.useEditorFontSize);
      } else {
        // context view
        var contextualDescription = content.getRight();
        var contextualTabFolder = new TabFolder(tabContent, SWT.NONE);
        for (var contextualTab : contextualDescription.getContextualSections()) {
          // context description
          var contextualTabItem = new TabItem(contextualTabFolder, SWT.NONE);
          contextualTabItem.setText(contextualTab.getDisplayName());
          var contextualTabContent = new Composite(contextualTabFolder, SWT.NONE);
          contextualTabContent.setLayout(new GridLayout(1, false));

          if (Objects.equals(contextualDescription.getDefaultContextKey(), contextualTab.getContextKey())) {
            contextualTabFolder.setSelection(contextualTabItem);
          }

          HTMLUtils.parseIntoElements(contextualTab.getHtmlContent(), contextualTabContent, languageKey,
            this.useEditorFontSize);
          contextualTabItem.setControl(contextualTabContent);
        }
        contextualTabFolder.requestLayout();
      }
      tabItem.setControl(tabContent);
    }
    tabFolder.requestLayout();
  }
}
