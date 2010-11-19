/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui.preferences;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;

import com.google.common.collect.Lists;

public class SonarUiPreferenceInitializer extends AbstractPreferenceInitializer {

  private static final String PREF_FAVOURITE_METRICS = "favouriteMetrics";

  private static final String KEY_METRICS = "metrics";
  private static final String KEY_METRIC = "metric";
  private static final String KEY_METRIC_KEY = "key";
  private static final String KEY_METRIC_NAME = "name";

  public static List<ISonarMetric> getDefaultFavouriteMetrics() {
    return Arrays.asList(SonarCorePlugin.createSonarMetric("complexity", "Complexity"),
        SonarCorePlugin.createSonarMetric("violations", "Violations"),
        SonarCorePlugin.createSonarMetric("duplicated_lines", "Duplicated lines"),
        SonarCorePlugin.createSonarMetric("uncovered_lines", "Uncovered lines"));
  }

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = SonarUiPlugin.getDefault().getPreferenceStore();
    store.setDefault(PREF_FAVOURITE_METRICS, createFavouriteMetricsMemento(getDefaultFavouriteMetrics()));
  }

  public static Collection<ISonarMetric> getFavouriteMetrics() {
    return getFavouriteMetricsFromMemento(SonarUiPlugin.getDefault().getPreferenceStore().getString(PREF_FAVOURITE_METRICS));
  }

  /**
   * Restores default favourite metrics.
   */
  public static void restoreDefaultFavouriteMetrics() {
    setFavouriteMetrics(getDefaultFavouriteMetrics());
  }

  public static synchronized void setFavouriteMetrics(Collection<ISonarMetric> metrics) {
    String memento = createFavouriteMetricsMemento(metrics);
    if (memento != null) {
      SonarUiPlugin.getDefault().getPreferenceStore().setValue(PREF_FAVOURITE_METRICS, memento);
    }
  }

  private static String createFavouriteMetricsMemento(Collection<ISonarMetric> metrics) {
    XMLMemento rootMemento = XMLMemento.createWriteRoot(KEY_METRICS);
    for (ISonarMetric metric : metrics) {
      IMemento memento = rootMemento.createChild(KEY_METRIC);
      memento.putString(KEY_METRIC_KEY, metric.getKey());
      memento.putString(KEY_METRIC_NAME, metric.getName());
    }
    return PlatformUtils.convertMementoToString(rootMemento);
  }

  private static Collection<ISonarMetric> getFavouriteMetricsFromMemento(String mementoString) {
    List<ISonarMetric> metrics = Lists.newArrayList();
    try {
      XMLMemento rootMemento = XMLMemento.createReadRoot(new StringReader(mementoString));
      for (IMemento memento : rootMemento.getChildren(KEY_METRIC)) {
        String key = memento.getString(KEY_METRIC_KEY);
        String name = memento.getString(KEY_METRIC_NAME);
        metrics.add(SonarCorePlugin.createSonarMetric(key, name));
      }
    } catch (WorkbenchException e) {
      // TODO handle
    }
    return metrics;
  }

}
