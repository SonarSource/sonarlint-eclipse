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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.commons.RuleKey;
import org.sonarsource.sonarlint.shaded.com.google.gson.Gson;
import org.sonarsource.sonarlint.shaded.com.google.gson.JsonParseException;
import org.sonarsource.sonarlint.shaded.com.google.gson.annotations.SerializedName;
import org.sonarsource.sonarlint.shaded.com.google.gson.reflect.TypeToken;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class SonarLintGlobalConfiguration {

  public static final String PREF_MARKER_SEVERITY = "markerSeverity"; //$NON-NLS-1$
  public static final int PREF_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_INFO;
  public static final String PREF_EXTRA_ARGS = "extraArgs"; //$NON-NLS-1$
  public static final String PREF_FILE_EXCLUSIONS = "fileExclusions"; //$NON-NLS-1$
  /**
   * @deprecated replaced by {@link #PREF_RULES_CONFIG}
   */
  @Deprecated
  public static final String PREF_RULE_EXCLUSIONS = "ruleExclusions"; //$NON-NLS-1$
  /**
   * @deprecated replaced by {@link #PREF_RULES_CONFIG}
   */
  @Deprecated
  public static final String PREF_RULE_INCLUSIONS = "ruleInclusions"; //$NON-NLS-1$
  public static final String PREF_RULES_CONFIG = "rulesConfig"; //$NON-NLS-1$
  public static final String PREF_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS = "testFileRegexps"; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES = "skipConfirmAnalyzeMultipleFiles"; //$NON-NLS-1$
  public static final String PREF_NODEJS_PATH = "nodeJsPath"; //$NON-NLS-1$
  private static final String PREF_TAINT_VULNERABILITY_DISPLAYED = "taintVulnerabilityDisplayed";
  private static final String PREF_SECRETS_EVER_DETECTED = "secretsEverDetected";

  private SonarLintGlobalConfiguration() {
    // Utility class
  }

  public static String getTestFileRegexps() {
    return Platform.getPreferencesService().getString(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_TEST_FILE_REGEXPS, PREF_TEST_FILE_REGEXPS_DEFAULT, null);
  }

  public static int getMarkerSeverity() {
    return Platform.getPreferencesService().getInt(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_MARKER_SEVERITY, PREF_MARKER_SEVERITY_DEFAULT, null);
  }

  public static List<SonarLintProperty> getExtraPropertiesForLocalAnalysis(ISonarLintProject project) {
    var props = new ArrayList<SonarLintProperty>();
    // First add all global properties
    var globalExtraArgs = getPreferenceString(PREF_EXTRA_ARGS);
    props.addAll(deserializeExtraProperties(globalExtraArgs));

    // Then add project properties
    var sonarProject = SonarLintCorePlugin.loadConfig(project);
    if (sonarProject.getExtraProperties() != null) {
      props.addAll(sonarProject.getExtraProperties());
    }

    return props;
  }

  public static List<SonarLintProperty> deserializeExtraProperties(@Nullable String property) {
    return Stream.of(StringUtils.split(property, "\r\n"))
      .map(keyValuePair -> StringUtils.split(keyValuePair, "="))
      .map(keyValue -> new SonarLintProperty(keyValue[0], keyValue.length > 1 ? keyValue[1] : ""))
      .collect(Collectors.toList());
  }

  public static String serializeFileExclusions(List<ExclusionItem> exclusions) {
    return exclusions.stream()
      .map(ExclusionItem::toStringWithType)
      .collect(Collectors.joining("\r\n"));
  }

  public static String serializeExtraProperties(List<SonarLintProperty> properties) {
    var keyValuePairs = properties.stream()
      .map(p -> p.getName() + "=" + p.getValue())
      .collect(Collectors.toList());
    return StringUtils.joinSkipNull(keyValuePairs, "\r\n");
  }

  public static List<ExclusionItem> deserializeFileExclusions(@Nullable String property) {
    return Stream.of(StringUtils.split(property, "\r\n"))
      .map(ExclusionItem::parse)
      .filter(Objects::nonNull)
      .collect(toList());
  }

  public static List<ExclusionItem> getGlobalExclusions() {
    // add globally-configured exclusions
    var props = getPreferenceString(SonarLintGlobalConfiguration.PREF_FILE_EXCLUSIONS);
    return deserializeFileExclusions(props);
  }

  private static void savePreferences(Consumer<Preferences> updater, String key, Object value) {
    var preferences = getInstancePreferenceNode();
    updater.accept(preferences);
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Could not save preference: " + key + " = " + value, e);
    }
  }

  private static IEclipsePreferences getInstancePreferenceNode() {
    return ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
  }

  private static String getPreferenceString(String key) {
    return Platform.getPreferencesService().getString(SonarLintCorePlugin.UI_PLUGIN_ID, key, PREF_DEFAULT, null);
  }

  private static void setPreferenceString(String key, String value) {
    savePreferences(p -> p.put(key, value), key, value);
  }

  private static boolean getPreferenceBoolean(String key) {
    return Platform.getPreferencesService().getBoolean(SonarLintCorePlugin.UI_PLUGIN_ID, key, false, null);
  }

  private static void setPreferenceBoolean(String key, boolean value) {
    savePreferences(p -> p.putBoolean(key, value), key, value);
  }

  private static String serializeRuleKeyList(Collection<RuleKey> exclusions) {
    return exclusions.stream()
      .map(RuleKey::toString)
      .sorted()
      .collect(Collectors.joining(";"));
  }

  public static void disableRule(RuleKey ruleKey) {
    var rules = new ArrayList<>(readRulesConfig());
    var ruleToDisable = rules.stream().filter(r -> r.getKey().equals(ruleKey.toString())).findFirst();
    if (ruleToDisable.isPresent()) {
      ruleToDisable.get().setActive(false);
    } else {
      rules.add(new RuleConfig(ruleKey.toString(), false));
    }
    saveRulesConfig(rules);
  }

  public static Collection<RuleKey> getExcludedRules() {
    return readRulesConfig().stream()
      .filter(r -> !r.isActive())
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  public static void setExcludedRules(Collection<RuleKey> excludedRules) {
    setPreferenceString(PREF_RULE_EXCLUSIONS, serializeRuleKeyList(excludedRules));
  }

  public static Collection<RuleKey> getIncludedRules() {
    return readRulesConfig().stream()
      .filter(RuleConfig::isActive)
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  public static void setIncludedRules(Collection<RuleKey> includedRules) {
    setPreferenceString(PREF_RULE_INCLUSIONS, serializeRuleKeyList(includedRules));
  }

  private static Set<RuleKey> deserializeRuleKeyList(@Nullable String property) {
    return Stream.of(StringUtils.split(property, ";"))
      .map(RuleKey::parse)
      .collect(Collectors.toSet());
  }

  public static Set<RuleConfig> readRulesConfig() {
    var instancePreferenceNode = getInstancePreferenceNode();
    try {
      if (!instancePreferenceNode.nodeExists(PREF_RULES_CONFIG)) {
        var result = new ArrayList<RuleConfig>();
        // Migration
        if (List.of(instancePreferenceNode.keys()).contains(PREF_RULE_EXCLUSIONS)) {
          deserializeRuleKeyList(getPreferenceString(PREF_RULE_EXCLUSIONS)).stream().map(r -> new RuleConfig(r.toString(), false)).forEach(result::add);
          instancePreferenceNode.remove(PREF_RULE_EXCLUSIONS);
        }
        if (List.of(instancePreferenceNode.keys()).contains(PREF_RULE_INCLUSIONS)) {
          deserializeRuleKeyList(getPreferenceString(PREF_RULE_INCLUSIONS)).stream().map(r -> new RuleConfig(r.toString(), true)).forEach(result::add);
          instancePreferenceNode.remove(PREF_RULE_INCLUSIONS);
        }
        if (!result.isEmpty()) {
          saveRulesConfig(result);
        }
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to migrate rules configuration", e);
    }
    var json = getPreferenceString(PREF_RULES_CONFIG);
    return deserializeRulesJson(json);
  }

  private static class RuleConfigGson {

    enum Level {
      @SerializedName("on")
      ON,
      @SerializedName("off")
      OFF
    }

    Level level = Level.ON;
    Map<String, String> parameters;

  }

  private static Set<RuleConfig> deserializeRulesJson(String json) {
    if (StringUtils.isBlank(json)) {
      return Collections.emptySet();
    }
    var mapType = new TypeToken<Map<String, RuleConfigGson>>() {
    }.getType();
    Map<String, RuleConfigGson> rulesByKey = new Gson().fromJson(json, mapType);
    var result = new HashSet<RuleConfig>();
    try {
      rulesByKey.forEach((key, config) -> {
        var rule = new RuleConfig(key, RuleConfigGson.Level.OFF != config.level);
        if (config.parameters != null) {
          config.parameters.forEach((k, v) -> rule.getParams().put(k, v));
        }
        result.add(rule);
      });
    } catch (JsonParseException e) {
      SonarLintLogger.get().error("Invalid JSON format for rules configuration", e);
    }
    return result;
  }

  public static void saveRulesConfig(Collection<RuleConfig> rules) {
    var json = serializeRulesJson(rules);
    setPreferenceString(PREF_RULES_CONFIG, json);
  }

  private static String serializeRulesJson(Collection<RuleConfig> rules) {
    Map<String, RuleConfigGson> rulesByKey = new LinkedHashMap<>();
    rules.stream()
      // Sort by key to ensure consistent results
      .sorted(comparing(RuleConfig::getKey))
      .forEach(rule -> {
        var ruleJson = new RuleConfigGson();
        ruleJson.level = rule.isActive() ? RuleConfigGson.Level.ON : RuleConfigGson.Level.OFF;
        if (!rule.getParams().isEmpty()) {
          ruleJson.parameters = new LinkedHashMap<>();
          rule.getParams().entrySet().stream()
            // Sort by key to ensure consistent results
            .sorted(comparing(Entry<String, String>::getKey))
            .forEach(param -> ruleJson.parameters.put(param.getKey(), param.getValue()));
        }
        rulesByKey.put(rule.getKey(), ruleJson);
      });
    var mapType = new TypeToken<Map<String, RuleConfigGson>>() {
    }.getType();
    return new Gson().toJson(rulesByKey, mapType);
  }

  public static void setSkipConfirmAnalyzeMultipleFiles() {
    setPreferenceBoolean(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES, true);
  }

  public static boolean skipConfirmAnalyzeMultipleFiles() {
    return getPreferenceBoolean(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES);
  }

  public static void setNodeJsPath(String path) {
    setPreferenceString(PREF_NODEJS_PATH, path);
  }

  public static String getNodejsPath() {
    return getPreferenceString(PREF_NODEJS_PATH);
  }

  public static boolean taintVulnerabilityNeverBeenDisplayed() {
    return !getPreferenceBoolean(PREF_TAINT_VULNERABILITY_DISPLAYED);
  }

  public static void setTaintVulnerabilityDisplayed() {
    setPreferenceBoolean(PREF_TAINT_VULNERABILITY_DISPLAYED, true);
  }

  public static boolean secretsNeverDetected() {
    return !getPreferenceBoolean(PREF_SECRETS_EVER_DETECTED);
  }

  public static void setSecretsWereDetected() {
    setPreferenceBoolean(PREF_SECRETS_EVER_DETECTED, true);
  }

}
