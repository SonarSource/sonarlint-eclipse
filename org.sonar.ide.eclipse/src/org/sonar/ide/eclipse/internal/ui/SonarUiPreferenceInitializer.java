package org.sonar.ide.eclipse.internal.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SonarUiPreferenceInitializer extends AbstractPreferenceInitializer {

  public static final String PREF_FAVOURITE_METRICS = ISonarConstants.PLUGIN_ID + ".favouriteMetrics";

  private static final String KEY_METRICS = "metrics";
  private static final String KEY_METRIC = "metric";
  private static final String KEY_METRIC_KEY = "key";
  private static final String KEY_METRIC_NAME = "name";

  public static List<ISonarMetric> getDefaults() {
    return Arrays.asList(
      SonarCorePlugin.createSonarMetric("complexity", "Complexity"),
      SonarCorePlugin.createSonarMetric("violations", "Violations"),
      SonarCorePlugin.createSonarMetric("duplicated_lines", "Duplicated lines"),
      SonarCorePlugin.createSonarMetric("uncovered_lines", "Uncovered lines"));
  }

  @Override
  public void initializeDefaultPreferences() {
    SonarUiPlugin.getDefault().getPreferenceStore().setDefault(PREF_FAVOURITE_METRICS, createFavouriteMetricsMemento(getDefaults()));
  }

  public static Collection<ISonarMetric> getFavouriteMetrics() {
    return getFavouriteMetricsFromMemento(SonarUiPlugin.getDefault().getPreferenceStore().getString(PREF_FAVOURITE_METRICS));
  }

  /**
   * Restores default favourite metrics.
   */
  public static void restoreDefaultFavouriteMetrics() {
    setFavouriteMetrics(getDefaults());
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
    String memento = null;
    try {
      StringWriter writer = new StringWriter();
      rootMemento.save(writer);
      memento = writer.getBuffer().toString();
    } catch (IOException e) {
      // TODO handle
    }
    return memento;
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
