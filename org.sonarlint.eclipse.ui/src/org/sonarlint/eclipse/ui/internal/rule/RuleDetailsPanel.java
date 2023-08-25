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

import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.ui.internal.properties.RulesConfigurationPage;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.AbstractRuleDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.EffectiveRuleDetailsDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleSplitDescriptionDto;

/** Panel containing the rule title, details and description */
public class RuleDetailsPanel extends Composite {
  private final Label ruleNameLabel;
  @Nullable
  private AbstractRuleHeaderPanel ruleHeaderPanel;
  @Nullable
  private RuleDescriptionPanel ruleDescriptionPanel;
  private final boolean useEditorFontSize;
  @Nullable
  private Group ruleParamsPanel;
  private final Composite scrolledContent;
  private final ScrolledComposite scrollComposite;

  public RuleDetailsPanel(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(1, false));

    scrollComposite = new ScrolledComposite(this, SWT.V_SCROLL);
    scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrollComposite.setExpandHorizontal(true);
    scrollComposite.setExpandVertical(true);

    scrolledContent = new Composite(scrollComposite, SWT.NONE);
    scrolledContent.setLayout(new GridLayout(1, false));
    scrollComposite.setContent(scrolledContent);

    this.useEditorFontSize = useEditorFontSize;

    ruleNameLabel = new Label(scrolledContent, SWT.NONE);
    ruleNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    var nameLabelFont = FontDescriptor.createFrom(ruleNameLabel.getFont())
      .setStyle(SWT.BOLD)
      .increaseHeight(3)
      .createFont(ruleNameLabel.getDisplay());
    ruleNameLabel.setFont(nameLabelFont);
    ruleNameLabel.addDisposeListener(e -> nameLabelFont.dispose());

    updateScrollCompositeMinSize();
    scrollComposite.addControlListener(new ControlListener() {
      @Override
      public void controlMoved(ControlEvent e) {
        // The control cannot be moved
      }

      @Override
      public void controlResized(ControlEvent e) {
        updateScrollCompositeMinSize();
      }
    });
  }

  private void updateScrollCompositeMinSize() {
    final var width = scrollComposite.getClientArea().width;
    scrollComposite.setMinSize(scrolledContent.computeSize(width, SWT.DEFAULT));
  }
  
  public void updateRule(AbstractRuleDto ruleInformation, Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description) {
    try {
      ruleNameLabel.setText(ruleInformation.getName());
      ruleNameLabel.requestLayout();

      updateHeader(ruleInformation);

      updateHtmlDescription(description, ruleInformation.getLanguage().getLanguageKey());
      
      if (ruleInformation instanceof EffectiveRuleDetailsDto) {
        updateParameters((EffectiveRuleDetailsDto) ruleInformation);
      }

      requestLayout();
      updateScrollCompositeMinSize();
    } catch (SWTException ignored) {
      // There might be a race condition between the background job running late and the view already being closed
    }
  }
  
  private void updateHtmlDescription(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description, String languageKey) {
    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
    ruleDescriptionPanel = new RuleDescriptionPanel(scrolledContent, languageKey, useEditorFontSize);
    ruleDescriptionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    ruleDescriptionPanel.updateRule(description);
  }
  
  private void updateHeader(AbstractRuleDto ruleInformation) {
    if (ruleHeaderPanel != null && !ruleHeaderPanel.isDisposed()) {
      ruleHeaderPanel.dispose();
    }
    
    var attributeOptional = ruleInformation.getCleanCodeAttribute();
    var impacts = ruleInformation.getDefaultImpacts();
    
    if (attributeOptional.isPresent() && !impacts.isEmpty()) {
      ruleHeaderPanel = new RuleHeaderPanel(scrolledContent);
    } else {
      ruleHeaderPanel = new LegacyRuleHeaderPanel(scrolledContent);
    }
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    
    ruleHeaderPanel.updateRule(ruleInformation);
  }

  private void updateParameters(EffectiveRuleDetailsDto details) {
    if (ruleParamsPanel != null && !ruleParamsPanel.isDisposed()) {
      ruleParamsPanel.dispose();
    }
    if (!details.getParams().isEmpty()) {
      ruleParamsPanel = new Group(scrolledContent, SWT.NONE);
      ruleParamsPanel.setText("Parameters");
      ruleParamsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

      ruleParamsPanel.setLayout(new GridLayout(2, false));

      var link = new Link(ruleParamsPanel, SWT.NONE);
      link.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      link.setText("Parameter values can be set in <a>Rules Configuration</a>. In connected mode, server-side configuration overrides local settings.");
      link.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT));
      link.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {
          PreferencesUtil.createPreferenceDialogOn(
            getShell(), RulesConfigurationPage.RULES_CONFIGURATION_ID,
            new String[] {RulesConfigurationPage.RULES_CONFIGURATION_ID}, null).open();
        }
      });

      for (var param : details.getParams()) {
        var paramDefaultValue = param.getDefaultValue();
        var defaultValue = paramDefaultValue != null ? paramDefaultValue : "(none)";
        var currentValue = param.getValue();
        var paramName = new Label(ruleParamsPanel, SWT.BOLD);
        paramName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        paramName.setFont(JFaceResources.getFontRegistry().getBold(
          JFaceResources.DIALOG_FONT));
        paramName.setText(param.getName());
        paramName.setToolTipText(param.getDescription());
        var paramValue = new Label(ruleParamsPanel, SWT.LEFT);
        var paramValueLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        paramValueLayoutData.horizontalIndent = 5;
        paramValue.setLayoutData(paramValueLayoutData);
        paramValue.setText(currentValue + (!Objects.equals(defaultValue, currentValue) ? (" (default: " + defaultValue + ")") : ""));
      }
    }
  }

  public void displayLoadingIndicator() {
    ruleNameLabel.setText("Loading...");
    ruleNameLabel.requestLayout();
  }

  public void clearRule() {
    ruleNameLabel.setText("No rules selected");
    ruleNameLabel.requestLayout();
    
    if (ruleHeaderPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleHeaderPanel.dispose();
    }

    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
  }
}
