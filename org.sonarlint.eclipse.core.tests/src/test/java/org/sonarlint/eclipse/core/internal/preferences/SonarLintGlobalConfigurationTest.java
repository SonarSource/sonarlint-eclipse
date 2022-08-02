/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
import org.sonarsource.sonarlint.core.commons.RuleKey;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintGlobalConfigurationTest {

  @Before
  public void clean() throws BackingStoreException {
    ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID).removeNode();
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
      new SonarLintProperty("key2", "value2"));

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
  public void should_serialize_and_deserialize_excluded_rules() {
    var excludedRules = List.of(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    SonarLintGlobalConfiguration.setExcludedRules(excludedRules);
    var deserialized = SonarLintGlobalConfiguration.getExcludedRules();

    assertThat(deserialized).containsExactlyInAnyOrderElementsOf(excludedRules);
  }

  @Test
  public void should_serialize_and_deserialize_included_rules() {
    var includedRules = List.of(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    SonarLintGlobalConfiguration.setIncludedRules(includedRules);
    var deserialized = SonarLintGlobalConfiguration.getIncludedRules();

    assertThat(deserialized).containsExactlyInAnyOrderElementsOf(includedRules);
  }

  @Test
  public void should_exclude_rule() {
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEmpty();

    var ruleKey1 = new RuleKey("squid", "S123");
    SonarLintGlobalConfiguration.disableRule(ruleKey1);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsOnly(ruleKey1);

    var ruleKey2 = new RuleKey("php", "S456");
    SonarLintGlobalConfiguration.disableRule(ruleKey2);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsExactlyInAnyOrder(ruleKey1, ruleKey2);
  }

  @Test
  public void should_ignore_duplicate_excluded_rules() {
    var ruleKey1 = new RuleKey("squid", "S123");
    var ruleKey2 = new RuleKey("php", "S456");
    var excludedRules = List.of(ruleKey1, ruleKey2);
    SonarLintGlobalConfiguration.setExcludedRules(excludedRules);

    var orig = SonarLintGlobalConfiguration.getExcludedRules();

    SonarLintGlobalConfiguration.disableRule(ruleKey1);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEqualTo(orig);

    SonarLintGlobalConfiguration.disableRule(ruleKey2);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEqualTo(orig);
  }

  @Test
  public void should_store_rule_keys_in_consistent_order() {
    var ruleKey1 = new RuleKey("squid", "S123");
    var ruleKey2 = new RuleKey("php", "S456");
    var ordering1 = List.of(ruleKey1, ruleKey2);
    var ordering2 = List.of(ruleKey2, ruleKey1);

    SonarLintGlobalConfiguration.setExcludedRules(ordering1);
    var deserialized1 = SonarLintGlobalConfiguration.getExcludedRules();

    SonarLintGlobalConfiguration.setExcludedRules(ordering2);
    var deserialized2 = SonarLintGlobalConfiguration.getExcludedRules();

    assertThat(deserialized1).isEqualTo(deserialized2);
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

  @Test
  public void testRulesConfigMigration() throws BackingStoreException {
    var configNode = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    configNode.put(SonarLintGlobalConfiguration.PREF_RULE_EXCLUSIONS, "Web:S5255;php:S2010");
    configNode.put(SonarLintGlobalConfiguration.PREF_RULE_INCLUSIONS, "Web:InputWithoutLabelCheck;Web:UnclosedTagCheck");
    configNode.flush();

    var rules = SonarLintGlobalConfiguration.readRulesConfig();
    assertThat(rules)
      .extracting(RuleConfig::getKey, RuleConfig::isActive, RuleConfig::getParams)
      .containsOnly(
        tuple("Web:S5255", false, emptyMap()),
        tuple("php:S2010", false, emptyMap()),
        tuple("Web:InputWithoutLabelCheck", true, emptyMap()),
        tuple("Web:UnclosedTagCheck", true, emptyMap()));

    assertThat(configNode.keys()).containsOnly(SonarLintGlobalConfiguration.PREF_RULES_CONFIG);
  }
}
