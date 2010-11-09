package org.sonar.ide.eclipse.core;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

public class FavouriteMetricsManager {

  public static FavouriteMetricsManager INSTANCE;

  public static FavouriteMetricsManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FavouriteMetricsManager();
    }
    return INSTANCE;
  }

  private List<String> metrics = Lists.newArrayList();

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
  }

}
