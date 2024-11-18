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

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
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
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.EffectiveIssueDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.EffectiveRuleParamDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleSplitDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;

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

  public void updateRule(EffectiveIssueDetailsDto details) {
    try {
      ruleNameLabel.setText(details.getName());
      ruleNameLabel.requestLayout();

      updateHeader(details);
      updateHtmlDescription(details.getDescription(), details.getLanguage());
      updateParameters(details.getParams());
    } catch (SWTException ignored) {
      // There might be a race condition between the background job running late and the view already being closed
    }
  }

  public void updateRule(RuleDefinitionDto definition,
    Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description) {
    try {
      ruleNameLabel.setText(definition.getName());
      ruleNameLabel.requestLayout();

      updateHeader(definition);
      updateHtmlDescription(description, definition.getLanguage());
    } catch (SWTException ignroed) {
      // There might be a race condition between the background job running late and the view already being closed
    }
  }

  private void updateHtmlDescription(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description, Language language) {
    if (ruleDescriptionPanel != null && !ruleDescriptionPanel.isDisposed()) {
      ruleDescriptionPanel.dispose();
    }
    ruleDescriptionPanel = new RuleDescriptionPanel(scrolledContent, language.name().toLowerCase(Locale.ENGLISH), useEditorFontSize);
    ruleDescriptionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    ruleDescriptionPanel.updateRule(description);
  }

  private void updateHeader(EffectiveIssueDetailsDto issueDetails) {
    if (ruleHeaderPanel != null && !ruleHeaderPanel.isDisposed()) {
      ruleHeaderPanel.dispose();
    }

    var ruleKey = issueDetails.getRuleKey();
    var severityDetails = issueDetails.getSeverityDetails();
    if (severityDetails.isLeft()) {
      ruleHeaderPanel = new LegacyRuleHeaderPanel(scrolledContent, severityDetails.getLeft(), ruleKey);
    } else {
      var mqrDetails = severityDetails.getRight();
      ruleHeaderPanel = new RuleHeaderPanel(scrolledContent, mqrDetails.getCleanCodeAttribute(),
        mqrDetails.getImpacts(), ruleKey);
    }
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    ruleHeaderPanel.updateRule();
  }

  private void updateHeader(RuleDefinitionDto definition) {
    if (ruleHeaderPanel != null && !ruleHeaderPanel.isDisposed()) {
      ruleHeaderPanel.dispose();
    }

    ruleHeaderPanel = new RuleHeaderPanel(scrolledContent, definition.getCleanCodeAttribute(),
      definition.getSoftwareImpacts(), definition.getKey());
    ruleHeaderPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    ruleHeaderPanel.updateRule();
  }

  private void updateParameters(Collection<EffectiveRuleParamDto> params) {
    if (ruleParamsPanel != null && !ruleParamsPanel.isDisposed()) {
      ruleParamsPanel.dispose();
    }

    if (!params.isEmpty()) {
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
          PreferencesUtil.createPreferenceDialogOn(getShell(), RulesConfigurationPage.RULES_CONFIGURATION_ID,
            null, null).open();
        }
      });

      for (var param : params) {
        var paramDefaultValue = param.getDefaultValue();
        var defaultValue = paramDefaultValue != null ? paramDefaultValue : "(none)";

        // When in Connected Mode the rules configuration from the server applies and therefore also the parameters.
        // When in Standalone Mode this information is saved locally and must be retrieved for the specific parameter!
        var currentValue = param.getValue() != null ? param.getValue() : param.getDefaultValue();

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
