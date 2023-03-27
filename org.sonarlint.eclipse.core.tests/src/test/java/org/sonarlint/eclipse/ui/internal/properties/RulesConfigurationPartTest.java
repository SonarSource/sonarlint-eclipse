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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.commons.Language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RulesConfigurationPartTest {

  private static final String ACTIVE = mockRuleKey("ACTIVE");
  private static final String ACTIVE_EXCLUDED = mockRuleKey("ACTIVE_EXCLUDED");
  private static final String INACTIVE = mockRuleKey("INACTIVE");
  private static final String INACTIVE_INCLUDED = mockRuleKey("INACTIVE_INCLUDED");

  private RulesConfigurationPart newSampleConfigurationPart() {
    Map<String, RuleDefinitionDto> allRuleDefinitions = Map.of(
      ACTIVE, mockRuleDetails(ACTIVE, true),
      ACTIVE_EXCLUDED, mockRuleDetails(ACTIVE_EXCLUDED, true),
      INACTIVE, mockRuleDetails(INACTIVE, false),
      INACTIVE_INCLUDED, mockRuleDetails(INACTIVE_INCLUDED, false));

    var ruleConfig = List.of(new RuleConfig(ACTIVE_EXCLUDED.toString(), false), new RuleConfig(INACTIVE_INCLUDED.toString(), true));
    return new RulesConfigurationPart(allRuleDefinitions, ruleConfig);
  }

  private static String mockRuleKey(String key) {
    return "java:" + key;
  }

  private RuleDefinitionDto mockRuleDetails(String ruleKey, boolean activeByDefault) {
    return new RuleDefinitionDto(ruleKey, null, null, null, null, activeByDefault, Language.JAVA);
  }

  @Test
  public void merges_exclusions_correctly() {
    var underTest = newSampleConfigurationPart();

    underTest.loadRules();

    var rules = underTest.computeRulesConfig();
    assertThat(rules).extracting(RuleConfig::getKey, RuleConfig::isActive).containsOnly(tuple(ACTIVE_EXCLUDED.toString(), false), tuple(INACTIVE_INCLUDED.toString(), true));
  }

  @Test
  public void reset_to_defaults_clears_exclusions_and_inclusions() {
    var underTest = newSampleConfigurationPart();

    underTest.resetToDefaults();

    var rules = underTest.computeRulesConfig();
    assertThat(rules).isEmpty();
  }
}
