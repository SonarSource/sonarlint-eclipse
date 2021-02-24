/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

import static java.util.Arrays.asList;
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
    List<ExclusionItem> list = new ArrayList<>();
    list.add(new ExclusionItem(Type.FILE, "file"));
    list.add(new ExclusionItem(Type.DIRECTORY, "dir"));

    String serialized = SonarLintGlobalConfiguration.serializeFileExclusions(list);
    List<ExclusionItem> desList = SonarLintGlobalConfiguration.deserializeFileExclusions(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_serialize_extra_properties() {
    List<SonarLintProperty> list = new ArrayList<>();
    list.add(new SonarLintProperty("key1", "value1"));
    list.add(new SonarLintProperty("key2", "value2"));

    String serialized = SonarLintGlobalConfiguration.serializeExtraProperties(list);
    List<SonarLintProperty> desList = SonarLintGlobalConfiguration.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  // SLE-267
  @Test
  public void should_serialize_extra_properties_empty_value() {
    List<SonarLintProperty> list = new ArrayList<>();
    list.add(new SonarLintProperty("key1", ""));
    list.add(new SonarLintProperty("key2", "value2"));

    String serialized = SonarLintGlobalConfiguration.serializeExtraProperties(list);
    List<SonarLintProperty> desList = SonarLintGlobalConfiguration.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_serialize_and_deserialize_excluded_rules() {
    List<RuleKey> excludedRules = asList(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    SonarLintGlobalConfiguration.setExcludedRules(excludedRules);
    Collection<RuleKey> deserialized = SonarLintGlobalConfiguration.getExcludedRules();

    assertThat(deserialized).containsExactlyInAnyOrder(excludedRules.toArray(new RuleKey[0]));
  }

  @Test
  public void should_serialize_and_deserialize_included_rules() {
    List<RuleKey> includedRules = asList(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    SonarLintGlobalConfiguration.setIncludedRules(includedRules);
    Collection<RuleKey> deserialized = SonarLintGlobalConfiguration.getIncludedRules();

    assertThat(deserialized).containsExactlyInAnyOrder(includedRules.toArray(new RuleKey[0]));
  }

  @Test
  public void should_exclude_rule() {
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEmpty();

    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    SonarLintGlobalConfiguration.disableRule(ruleKey1);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsOnly(ruleKey1);

    RuleKey ruleKey2 = new RuleKey("php", "S456");
    SonarLintGlobalConfiguration.disableRule(ruleKey2);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).containsExactlyInAnyOrder(ruleKey1, ruleKey2);
  }

  @Test
  public void should_ignore_duplicate_excluded_rules() {
    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    RuleKey ruleKey2 = new RuleKey("php", "S456");
    List<RuleKey> excludedRules = asList(ruleKey1, ruleKey2);
    SonarLintGlobalConfiguration.setExcludedRules(excludedRules);

    Collection<RuleKey> orig = SonarLintGlobalConfiguration.getExcludedRules();

    SonarLintGlobalConfiguration.disableRule(ruleKey1);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEqualTo(orig);

    SonarLintGlobalConfiguration.disableRule(ruleKey2);
    assertThat(SonarLintGlobalConfiguration.getExcludedRules()).isEqualTo(orig);
  }

  @Test
  public void should_store_rule_keys_in_consistent_order() {
    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    RuleKey ruleKey2 = new RuleKey("php", "S456");
    List<RuleKey> ordering1 = asList(ruleKey1, ruleKey2);
    List<RuleKey> ordering2 = asList(ruleKey2, ruleKey1);

    SonarLintGlobalConfiguration.setExcludedRules(ordering1);
    Collection<RuleKey> deserialized1 = SonarLintGlobalConfiguration.getExcludedRules();

    SonarLintGlobalConfiguration.setExcludedRules(ordering2);
    Collection<RuleKey> deserialized2 = SonarLintGlobalConfiguration.getExcludedRules();

    assertThat(deserialized1).isEqualTo(deserialized2);
  }

  @Test
  public void testRulesConfigSerializationRoundTrip() {
    Collection<RuleConfig> rules = SonarLintGlobalConfiguration.readRulesConfig();
    assertThat(rules).isEmpty();

    RuleConfig activeRule = new RuleConfig("active", true);
    RuleConfig inactiveRule = new RuleConfig("inactive", false);
    RuleConfig ruleWithParams = new RuleConfig("ruleWithParams", true);
    ruleWithParams.getParams().put("param1", "value1");
    ruleWithParams.getParams().put("param2", "value2");
    SonarLintGlobalConfiguration.saveRulesConfig(asList(activeRule, inactiveRule, ruleWithParams));

    IEclipsePreferences configNode = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    assertThat(configNode.get(SonarLintGlobalConfiguration.PREF_RULES_CONFIG, "")).isEqualTo(
      "{\"active\":{\"level\":\"on\"},\"inactive\":{\"level\":\"off\"},\"ruleWithParams\":{\"level\":\"on\",\"parameters\":{\"param1\":\"value1\",\"param2\":\"value2\"}}}");

    rules = SonarLintGlobalConfiguration.readRulesConfig();
    HashMap<Object, Object> expectedParams = new HashMap<>();
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
    IEclipsePreferences configNode = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    configNode.put(SonarLintGlobalConfiguration.PREF_RULE_EXCLUSIONS, "Web:S5255;php:S2010");
    configNode.put(SonarLintGlobalConfiguration.PREF_RULE_INCLUSIONS, "Web:InputWithoutLabelCheck;Web:UnclosedTagCheck");
    configNode.flush();

    Collection<RuleConfig> rules = SonarLintGlobalConfiguration.readRulesConfig();
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
