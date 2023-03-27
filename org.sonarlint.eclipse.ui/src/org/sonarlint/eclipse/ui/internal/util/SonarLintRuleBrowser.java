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

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.properties.RulesConfigurationPage;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.EffectiveRuleDetailsDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.EffectiveRuleParamDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleSplitDescriptionDto;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;

public class SonarLintRuleBrowser extends SonarLintWebView {

  @Nullable
  private Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> ruleDescription;
  @Nullable
  private String ruleName;
  @Nullable
  private String ruleKey;
  private Collection<EffectiveRuleParamDto> effectiveParams;
  private IssueSeverity severity;
  private RuleType type;

  public SonarLintRuleBrowser(Composite parent, boolean useEditorFontSize) {
    super(parent, useEditorFontSize);
  }

  @Override
  protected String body() {
    if (ruleDescription == null) {
      return "<small><em>(No rules selected)</em></small>";
    } else {
      var htmlDescription = renderDescription(ruleDescription);

      var ruleParamsMarkup = "";
      if (!effectiveParams.isEmpty()) {
        ruleParamsMarkup = "<div>" + renderRuleParams(effectiveParams) + "</div>";
      }
      var typeImg64 = getAsBase64(SonarLintImages.getTypeImage(type));
      var severityImg64 = getAsBase64(SonarLintImages.getSeverityImage(severity));
      return "<h1><span class=\"rulename\">"
        + escapeHTML(ruleName) + "</span><span class=\"rulekey\"> (" + ruleKey + ")</span></h1>"
        + "<div class=\"typeseverity\">"
        + "<img class=\"typeicon\" alt=\"" + type + "\" src=\"data:image/gif;base64," + typeImg64 + "\">"
        + "<span>" + clean(type.name()) + "</span>"
        + "<img class=\"severityicon\" alt=\"" + severity + "\" src=\"data:image/gif;base64," + severityImg64 + "\">"
        + "<span>" + clean(severity.name()) + "</span>"
        + "</div>"
        + htmlDescription
        + ruleParamsMarkup;
    }
  }

  private static String renderDescription(Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> ruleDescription) {
    if (ruleDescription.isLeft()) {
      return ruleDescription.getLeft().getHtmlContent();
    }
    var splittedDesc = ruleDescription.getRight();
    // FIXME handle tabs
    return splittedDesc.getIntroductionHtmlContent();
  }

  private static String renderRuleParams(Collection<EffectiveRuleParamDto> params) {
    return "<h2>Parameters</h2>" +
      "<p>" +
      "Following parameter values can be set in <a href='" + RulesConfigurationPage.RULES_CONFIGURATION_LINK + "'>Rules Configuration</a>.\n" +
      "</p>" +
      "<table class=\"rule-params\">" +
      params.stream().map(SonarLintRuleBrowser::renderRuleParam).collect(Collectors.joining("\n")) +
      "</table>";
  }

  private static String renderRuleParam(EffectiveRuleParamDto param) {
    var paramDescription = param.getDescription() != null ? param.getDescription() : "";
    var paramDefaultValue = param.getDefaultValue();
    var defaultValue = paramDefaultValue != null ? paramDefaultValue : "(none)";
    var currentValue = Optional.ofNullable(param.getValue()).orElse(defaultValue);
    return "<tr>" +
      "<th>" + param.getName() + "</th>" +
      "<td class='param-description'>" +
      paramDescription +
      "<p><small>Current value: <code>" + currentValue + "</code></small></p>" +
      "<p><small>Default value: <code>" + defaultValue + "</code></small></p>" +
      "</td>" +
      "</tr>";
  }

  public void clear() {
    this.ruleDescription = null;
    this.ruleName = null;
    this.ruleKey = null;
    this.effectiveParams = List.of();
    this.severity = null;
    this.type = null;
    refresh();
  }

  public void displayEffectiveRule(EffectiveRuleDetailsDto details) {
    this.ruleName = details.getName();
    this.ruleKey = details.getKey();
    this.ruleDescription = details.getDescription();
    this.effectiveParams = details.getParams();
    this.severity = details.getSeverity();
    this.type = details.getType();
    refresh();
  }

  public void displayStandaloneRule(RuleDefinitionDto ruleDef, Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> ruleDescription) {
    this.ruleName = ruleDef.getName();
    this.ruleKey = ruleDef.getKey();
    this.ruleDescription = ruleDescription;
    this.effectiveParams = List.of();
    this.severity = ruleDef.getDefaultSeverity();
    this.type = ruleDef.getType();
    refresh();
  }

  public static String escapeHTML(String s) {
    var out = new StringBuilder(Math.max(16, s.length()));
    for (var i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  private static String clean(@Nullable String txt) {
    if (txt == null) {
      return "";
    }
    return StringUtils.capitalize(txt.toLowerCase(Locale.ENGLISH).replace("_", " "));
  }

  private static String getAsBase64(@Nullable Image image) {
    if (image == null) {
      return "";
    }
    var out = new ByteArrayOutputStream();
    var loader = new ImageLoader();
    loader.data = new ImageData[] {image.getImageData()};
    loader.save(out, SWT.IMAGE_PNG);
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }

}
