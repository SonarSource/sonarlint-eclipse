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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class PreferencesUtils {

  public static final String PREF_MARKER_SEVERITY = "markerSeverity"; //$NON-NLS-1$
  public static final int PREF_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_INFO;
  public static final String PREF_EXTRA_ARGS = "extraArgs"; //$NON-NLS-1$
  public static final String PREF_FILE_EXCLUSIONS = "fileExclusions"; //$NON-NLS-1$
  public static final String PREF_RULE_EXCLUSIONS = "ruleExclusions"; //$NON-NLS-1$
  public static final String PREF_RULE_INCLUSIONS = "ruleInclusions"; //$NON-NLS-1$
  public static final String PREF_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS = "testFileRegexps"; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES = "skipConfirmAnalyzeMultipleFiles"; //$NON-NLS-1$

  private PreferencesUtils() {
    // Utility class
  }

  public static String getTestFileRegexps() {
    return Platform.getPreferencesService().getString(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_TEST_FILE_REGEXPS, PREF_TEST_FILE_REGEXPS_DEFAULT, null);
  }

  public static int getMarkerSeverity() {
    return Platform.getPreferencesService().getInt(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_MARKER_SEVERITY, PREF_MARKER_SEVERITY_DEFAULT, null);
  }

  public static List<SonarLintProperty> getExtraPropertiesForLocalAnalysis(ISonarLintProject project) {
    List<SonarLintProperty> props = new ArrayList<>();
    // First add all global properties
    String globalExtraArgs = getPreferenceString(PREF_EXTRA_ARGS);
    props.addAll(deserializeExtraProperties(globalExtraArgs));

    // Then add project properties
    SonarLintProjectConfiguration sonarProject = SonarLintCorePlugin.loadConfig(project);
    if (sonarProject.getExtraProperties() != null) {
      props.addAll(sonarProject.getExtraProperties());
    }

    return props;
  }

  public static List<SonarLintProperty> deserializeExtraProperties(@Nullable String property) {
    List<SonarLintProperty> props = new ArrayList<>();
    // First add all global properties
    String[] keyValuePairs = StringUtils.split(property, "\r\n");
    for (String keyValuePair : keyValuePairs) {
      String[] keyValue = StringUtils.split(keyValuePair, "=");
      props.add(new SonarLintProperty(keyValue[0], keyValue.length > 1 ? keyValue[1] : ""));
    }

    return props;
  }

  public static String serializeFileExclusions(List<ExclusionItem> exclusions) {
    return exclusions.stream()
      .map(ExclusionItem::toStringWithType)
      .collect(Collectors.joining("\r\n"));
  }

  public static String serializeExtraProperties(List<SonarLintProperty> properties) {
    List<String> keyValuePairs = new ArrayList<>(properties.size());
    for (SonarLintProperty prop : properties) {
      keyValuePairs.add(prop.getName() + "=" + prop.getValue());
    }
    return StringUtils.joinSkipNull(keyValuePairs, "\r\n");
  }

  public static List<ExclusionItem> deserializeFileExclusions(@Nullable String property) {
    String[] values = StringUtils.split(property, "\r\n");
    return Arrays.stream(values)
      .map(ExclusionItem::parse)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public static List<ExclusionItem> getGlobalExclusions() {
    // add globally-configured exclusions
    String props = getPreferenceString(PreferencesUtils.PREF_FILE_EXCLUSIONS);
    return deserializeFileExclusions(props);
  }

  private static void savePreferences(Consumer<Preferences> updater, String key, Object value) {
    Preferences preferences = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    updater.accept(preferences);
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Could not save preference: " + key + " = " + value, e);
    }
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

  private static Set<RuleKey> deserializeRuleKeyList(@Nullable String property) {
    String[] values = StringUtils.split(property, ";");
    return Arrays.stream(values)
      .map(RuleKey::parse)
      .collect(Collectors.toSet());
  }

  public static void excludeRule(RuleKey ruleKey) {
    Collection<RuleKey> excludedRules = getExcludedRules();
    excludedRules.add(ruleKey);
    setExcludedRules(excludedRules);
  }

  public static Collection<RuleKey> getExcludedRules() {
    return deserializeRuleKeyList(getPreferenceString(PREF_RULE_EXCLUSIONS));
  }

  public static void setExcludedRules(Collection<RuleKey> excludedRules) {
    setPreferenceString(PREF_RULE_EXCLUSIONS, serializeRuleKeyList(excludedRules));
  }

  public static Collection<RuleKey> getIncludedRules() {
    return deserializeRuleKeyList(getPreferenceString(PREF_RULE_INCLUSIONS));
  }

  public static void setIncludedRules(Collection<RuleKey> includedRules) {
    setPreferenceString(PREF_RULE_INCLUSIONS, serializeRuleKeyList(includedRules));
  }

  public static void setSkipConfirmAnalyzeMultipleFiles() {
    setPreferenceBoolean(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES, true);
  }

  public static boolean skipConfirmAnalyzeMultipleFiles() {
    return getPreferenceBoolean(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES);
  }
}
