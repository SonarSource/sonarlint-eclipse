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
package org.sonarlint.eclipse.core.internal.preferences;

import java.util.HashMap;
import java.util.List;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintGlobalConfigurationTest extends SonarTestCase {

  @Before
  public void clean() throws BackingStoreException {
    ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID).clear();
  }

  @Test
  public void should_serialize_file_exclusions() {
    var list = List.of(
      new ExclusionItem(Type.FILE, "file"),
      new ExclusionItem(Type.DIRECTORY, "dir"));

    var serialized = SonarLintGlobalConfiguration.serializeFileExclusions(list);
    var desList = SonarLintGlobalConfiguration.deserializeFileExclusions(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_serialize_extra_properties() {
    var list = List.of(
      new SonarLintProperty("key1", "value1"),
      new SonarLintProperty("key2   ", "value2   "));

    var serialized = SonarLintGlobalConfiguration.serializeExtraProperties(list);
    var desList = SonarLintGlobalConfiguration.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  // SLE-267
  @Test
  public void should_serialize_extra_properties_empty_value() {
    var list = List.of(
      new SonarLintProperty("key1", ""),
      new SonarLintProperty("key2", "value2"));

    var serialized = SonarLintGlobalConfiguration.serializeExtraProperties(list);
    var desList = SonarLintGlobalConfiguration.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_exclude_rule() {
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEmpty();

    var ruleKey1 = "squid:S123";
    SonarLintGlobalConfiguration.disableRule(ruleKey1);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsOnly(ruleKey1);

    var ruleKey2 = "php:S456";
    SonarLintGlobalConfiguration.disableRule(ruleKey2);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsExactlyInAnyOrder(ruleKey1, ruleKey2);
  }

  @Test
  public void testRulesConfigSerializationRoundTrip() {
    var rules = SonarLintGlobalConfiguration.readRulesConfig();
    assertThat(rules).isEmpty();

    var activeRule = new RuleConfig("active", true);
    var inactiveRule = new RuleConfig("inactive", false);
    var ruleWithParams = new RuleConfig("ruleWithParams", true);
    ruleWithParams.getParams().put("param1", "value1");
    ruleWithParams.getParams().put("param2", "value2");
    SonarLintGlobalConfiguration.saveRulesConfig(List.of(activeRule, inactiveRule, ruleWithParams));

    var configNode = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    assertThat(configNode.get(SonarLintGlobalConfiguration.PREF_RULES_CONFIG, "")).isEqualTo(
      "{\"active\":{\"level\":\"on\"},\"inactive\":{\"level\":\"off\"},\"ruleWithParams\":{\"level\":\"on\",\"parameters\":{\"param1\":\"value1\",\"param2\":\"value2\"}}}");

    rules = SonarLintGlobalConfiguration.readRulesConfig();
    var expectedParams = new HashMap<>();
    expectedParams.put("param1", "value1");
    expectedParams.put("param2", "value2");
    assertThat(rules)
      .extracting(RuleConfig::getKey, RuleConfig::isActive, RuleConfig::getParams)
      .containsOnly(
        tuple("active", true, emptyMap()),
        tuple("inactive", false, emptyMap()),
        tuple("ruleWithParams", true, expectedParams));

  }
}
