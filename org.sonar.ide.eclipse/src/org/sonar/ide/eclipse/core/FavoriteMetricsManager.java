package org.sonar.ide.eclipse.core;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.ListenerList;
import org.osgi.service.prefs.Preferences;

import com.google.common.collect.Lists;

public class FavoriteMetricsManager {

  public static FavoriteMetricsManager INSTANCE;

  public static FavoriteMetricsManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FavoriteMetricsManager();
    }
    return INSTANCE;
  }

  private List<String> metrics = Lists.newArrayList();
  private ListenerList listeners = new ListenerList();

  public List<String> get() {
    return Collections.unmodifiableList(metrics);
  }

  public boolean isFavorite(String metricKey) {
    return metrics.contains(metricKey);
  }

  public void toggle(String metricKey) {
    if (metrics.contains(metricKey)) {
      metrics.remove(metricKey);
    } else {
      metrics.add(metricKey);
    }
    for (Object o : listeners.getListeners()) {
      ((PropertyChangeListener) o).propertyChange(null);
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

  public void addListener(PropertyChangeListener listener) {
    listeners.add(listener);
  }

  public void removeListener(PropertyChangeListener listener) {
    listeners.remove(listener);
  }
}
