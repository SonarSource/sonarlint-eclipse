package org.sonar.ide.eclipse.core;

import org.eclipse.core.runtime.SafeRunner;
import org.sonar.ide.eclipse.ui.SonarUiPreferenceInitializer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class FavoriteMetricsManager {

  public static FavoriteMetricsManager INSTANCE;

  public static synchronized FavoriteMetricsManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FavoriteMetricsManager();
    }
    return INSTANCE;
  }

  private FavoriteMetricsManager() {
  }

  private Set<IFavouriteMetricsListener> listeners = Sets.newHashSet();

  private List<ISonarMetric> metrics = Lists.newArrayList();

  public List<ISonarMetric> get() {
    return Collections.unmodifiableList(metrics);
  }

  public boolean isFavorite(ISonarMetric metric) {
    return metrics.contains(metric);
  }

  public void set(Collection<ISonarMetric> metrics) {
    this.metrics.clear();
    this.metrics.addAll(metrics);
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

  public void addListener(IFavouriteMetricsListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IFavouriteMetricsListener listener) {
    listeners.remove(listener);
  }

  public void restoreDefaults() {
    set(SonarUiPreferenceInitializer.getDefaults());
  }

}
