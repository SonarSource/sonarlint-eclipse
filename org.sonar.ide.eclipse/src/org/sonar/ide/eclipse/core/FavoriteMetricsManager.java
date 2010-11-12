package org.sonar.ide.eclipse.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.SafeRunner;
import org.osgi.service.prefs.Preferences;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FavoriteMetricsManager {

  public static FavoriteMetricsManager INSTANCE;

  public static FavoriteMetricsManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FavoriteMetricsManager();
    }
    return INSTANCE;
  }

  private Set<IFavouriteMetricsListener> listeners = Sets.newHashSet();

  private List<String> metrics = Lists.newArrayList();

  public List<String> get() {
    return Collections.unmodifiableList(metrics);
  }

  public boolean isFavorite(String metricKey) {
    return metrics.contains(metricKey);
  }

  public void toggle(final String metricKey) {
    if (metrics.contains(metricKey)) {
      metrics.remove(metricKey);
      for (final IFavouriteMetricsListener listener : listeners) {
        SafeRunner.run(new AbstractSafeRunnable() {
          public void run() throws Exception {
            listener.metricRemoved(metricKey);
          }
        });
      }
    } else {
      metrics.add(metricKey);
      for (final IFavouriteMetricsListener listener : listeners) {
        SafeRunner.run(new AbstractSafeRunnable() {
          public void run() throws Exception {
            listener.metricRemoved(metricKey);
          }
        });
      }
    }
  }

  private static String PREFS_KEY = "favoriteMetrics";

  public void load(Preferences prefs) {
    String[] keys = StringUtils.split(prefs.get(PREFS_KEY, ""), ',');
    metrics.addAll(Arrays.asList(keys));
  }

  public void save(Preferences prefs) {
    prefs.put(PREFS_KEY, StringUtils.join(metrics, ','));
  }

  public void addListener(IFavouriteMetricsListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IFavouriteMetricsListener listener) {
    listeners.remove(listener);
  }

}
