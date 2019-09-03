/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;

public class PreferencesUtilsTest {

  @Before
  public void clean() throws BackingStoreException {
    ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID).removeNode();
  }

  @Test
  public void should_serialize_file_exclusions() {
    List<ExclusionItem> list = new ArrayList<>();
    list.add(new ExclusionItem(Type.FILE, "file"));
    list.add(new ExclusionItem(Type.DIRECTORY, "dir"));

    String serialized = PreferencesUtils.serializeFileExclusions(list);
    List<ExclusionItem> desList = PreferencesUtils.deserializeFileExclusions(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_serialize_extra_properties() {
    List<SonarLintProperty> list = new ArrayList<>();
    list.add(new SonarLintProperty("key1", "value1"));
    list.add(new SonarLintProperty("key2", "value2"));

    String serialized = PreferencesUtils.serializeExtraProperties(list);
    List<SonarLintProperty> desList = PreferencesUtils.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  // SLE-267
  @Test
  public void should_serialize_extra_properties_empty_value() {
    List<SonarLintProperty> list = new ArrayList<>();
    list.add(new SonarLintProperty("key1", ""));
    list.add(new SonarLintProperty("key2", "value2"));

    String serialized = PreferencesUtils.serializeExtraProperties(list);
    List<SonarLintProperty> desList = PreferencesUtils.deserializeExtraProperties(serialized);

    assertThat(desList).isEqualTo(list);
  }

  @Test
  public void should_serialize_and_deserialize_excluded_rules() {
    List<RuleKey> excludedRules = Arrays.asList(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    PreferencesUtils.setExcludedRules(excludedRules);
    Collection<RuleKey> deserialized = PreferencesUtils.getExcludedRules();

    assertThat(deserialized).containsExactlyInAnyOrder(excludedRules.toArray(new RuleKey[0]));
  }

  @Test
  public void should_serialize_and_deserialize_included_rules() {
    List<RuleKey> includedRules = Arrays.asList(new RuleKey("squid", "S123"), new RuleKey("php", "S456"));

    PreferencesUtils.setIncludedRules(includedRules);
    Collection<RuleKey> deserialized = PreferencesUtils.getIncludedRules();

    assertThat(deserialized).containsExactlyInAnyOrder(includedRules.toArray(new RuleKey[0]));
  }

  @Test
  public void should_exclude_rule() {
    assertThat(PreferencesUtils.getExcludedRules()).isEmpty();

    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    PreferencesUtils.excludeRule(ruleKey1);
    assertThat(PreferencesUtils.getExcludedRules()).isEqualTo(Collections.singleton(ruleKey1));

    RuleKey ruleKey2 = new RuleKey("php", "S456");
    PreferencesUtils.excludeRule(ruleKey2);
    assertThat(PreferencesUtils.getExcludedRules()).containsExactlyInAnyOrder(ruleKey1, ruleKey2);
  }

  @Test
  public void should_ignore_duplicate_excluded_rules() {
    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    RuleKey ruleKey2 = new RuleKey("php", "S456");
    List<RuleKey> excludedRules = Arrays.asList(ruleKey1, ruleKey2);
    PreferencesUtils.setExcludedRules(excludedRules);

    Collection<RuleKey> orig = PreferencesUtils.getExcludedRules();

    PreferencesUtils.excludeRule(ruleKey1);
    assertThat(PreferencesUtils.getExcludedRules()).isEqualTo(orig);

    PreferencesUtils.excludeRule(ruleKey2);
    assertThat(PreferencesUtils.getExcludedRules()).isEqualTo(orig);
  }

  @Test
  public void should_store_rule_keys_in_consistent_order() {
    RuleKey ruleKey1 = new RuleKey("squid", "S123");
    RuleKey ruleKey2 = new RuleKey("php", "S456");
    List<RuleKey> ordering1 = Arrays.asList(ruleKey1, ruleKey2);
    List<RuleKey> ordering2 = Arrays.asList(ruleKey2, ruleKey1);

    PreferencesUtils.setExcludedRules(ordering1);
    Collection<RuleKey> deserialized1 = PreferencesUtils.getExcludedRules();

    PreferencesUtils.setExcludedRules(ordering2);
    Collection<RuleKey> deserialized2 = PreferencesUtils.getExcludedRules();

    assertThat(deserialized1).isEqualTo(deserialized2);
  }
}
