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
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.util.SonarLintWebView;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetEffectiveRuleDetailsResponse;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleSplitDescriptionDto;

/**
 *  Panel containing the rule title, details and description
 *
 *  | Rule title                                                         |  -> StyledText
 *  | Type icon | Type label | Severity icon | Severity label | Rule key |  -> RuleHeaderPanel
 *  | Rule description                                                   |  -> SonarLintRuleBrowser
 */
public class RuleDetailsPanel extends Composite {

  private final CopyableLabel ruleNameLabel;
  private final RuleHeaderPanel ruleHeaderPanel;
  @Nullable
  private Composite descriptionComposite;
  private final Font nameLabelFont;
  private final boolean useEditorFontSize;

  public RuleDetailsPanel(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    this.useEditorFontSize = useEditorFontSize;

    var layout = new GridLayout(1, false);
    setLayout(layout);

    ruleNameLabel = new CopyableLabel(this);
    ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    nameLabelFont = FontDescriptor.createFrom(ruleNameLabel.getFont())
      .setStyle(SWT.BOLD)
      .increaseHeight(3)
      .createFont(ruleNameLabel.getDisplay());
    ruleNameLabel.setFont(nameLabelFont);

    ruleHeaderPanel = new RuleHeaderPanel(this);
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

    // TODO params
  }

  @Override
  public void dispose() {
    nameLabelFont.dispose();
    super.dispose();
  }

  public void updateRule(GetStandaloneRuleDescriptionResponse getStandaloneRuleDescriptionResponse) {
    var ruleDefinition = getStandaloneRuleDescriptionResponse.getRuleDefinition();

    ruleNameLabel.setText(ruleDefinition.getName());
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.update(ruleDefinition.getKey(), ruleDefinition.getType(), ruleDefinition.getDefaultSeverity());
    updateDescription(getStandaloneRuleDescriptionResponse.getDescription());
  }

  public void updateRule(GetEffectiveRuleDetailsResponse getEffectiveRuleDetailsResponse) {
    var details = getEffectiveRuleDetailsResponse.details();

    ruleNameLabel.setText(details.getName());
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.update(details.getKey(), details.getType(), details.getSeverity());
    updateDescription(details.getDescription());
  }

  private void updateDescription(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description) {
    if (descriptionComposite != null && !descriptionComposite.isDisposed()) {
      descriptionComposite.dispose();
    }
    descriptionComposite = new Composite(this, SWT.NONE);
    descriptionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    descriptionComposite.setLayout(new GridLayout());

    description.map(monolithDescription -> {
      var browser = new SonarLintWebView(descriptionComposite, useEditorFontSize);
      browser.setHtmlBody(monolithDescription.getHtmlContent());
      browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      return null;
    },
      withSections -> {
        var intro = withSections.getIntroductionHtmlContent();
        if (StringUtils.isNotBlank(intro)) {
          var introBrowser = new SonarLintWebView(descriptionComposite, useEditorFontSize);
          introBrowser.setHtmlBody(intro);
          introBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        }

        final var tabFolder = new TabFolder(descriptionComposite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        for (var tab : withSections.getTabs()) {
          var tabItem = new TabItem(tabFolder, SWT.NONE);
          tabItem.setText(tab.getTitle());
          var browser = new SonarLintWebView(tabFolder, useEditorFontSize);
          tab.getContent().map(nonContextual -> {
            browser.setHtmlBody(nonContextual.getHtmlContent());
            return null;
          }, contextual -> {
            return null;
          });

          tabItem.setControl(browser);
        }

        return null;
      });

    this.requestLayout();
  }

  public void clearRule() {
    ruleNameLabel.setText("No rule selected");
    ruleNameLabel.requestLayout();
    ruleHeaderPanel.clearRule();
    if (descriptionComposite != null && !descriptionComposite.isDisposed()) {
      descriptionComposite.dispose();
    }
  }
}
