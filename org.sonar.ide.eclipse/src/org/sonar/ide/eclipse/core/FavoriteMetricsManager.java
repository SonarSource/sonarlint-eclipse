package org.sonar.ide.eclipse.core;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.SafeRunner;
import org.osgi.service.prefs.Preferences;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FavoriteMetricsManager {

  public static FavoriteMetricsManager INSTANCE;

  public static FavoriteMetricsManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FavoriteMetricsManager();
    }
    return INSTANCE;
  }

  private Set<IFavouriteMetricsListener> listeners = Sets.newHashSet();

  private List<ISonarMetric> metrics = Lists.newArrayList();

  public List<ISonarMetric> get() {
    return Collections.unmodifiableList(metrics);
  }

  public boolean isFavorite(ISonarMetric metric) {
    return metrics.contains(metric);
  }

  public void set(List<ISonarMetric> list) {
    metrics.clear();
    metrics.addAll(list);
    notifyListeners();
  }

  public void toggle(final ISonarMetric metric) {
    if (metrics.contains(metric)) {
      metrics.remove(metric);
    } else {
      metrics.add(metric);
    }
    notifyListeners();
  }

  private void notifyListeners() {
    for (final IFavouriteMetricsListener listener : listeners) {
      SafeRunner.run(new AbstractSafeRunnable() {
        public void run() throws Exception {
          listener.updated();
        }
      });
    }
  }

  private static String PREFS_KEY = "favoriteMetrics";

  public void load(Preferences prefs) {
    String[] keys = StringUtils.split(prefs.get(PREFS_KEY, ""), ',');
    for (String key : keys) {
      metrics.add(SonarCorePlugin.createSonarMetric(key));
    }
  }

  public void save(Preferences prefs) {
    List<String> keys = Lists.transform(metrics, new Function<ISonarMetric, String>() {
      public String apply(ISonarMetric metric) {
        return metric.getKey();
      }
    });
    prefs.put(PREFS_KEY, StringUtils.join(keys, ','));
  }

  public void addListener(IFavouriteMetricsListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IFavouriteMetricsListener listener) {
    listeners.remove(listener);
  }

  public void restoreDefaults() {
    set(Arrays.asList(
      SonarCorePlugin.createSonarMetric("complexity", "Complexity"),
      SonarCorePlugin.createSonarMetric("violations", "Violations"),
      SonarCorePlugin.createSonarMetric("duplicated_lines", "Duplicated lines"),
      SonarCorePlugin.createSonarMetric("uncovered_lines", "Uncovered lines")));
  }

}
