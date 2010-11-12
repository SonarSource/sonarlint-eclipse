package org.sonar.ide.eclipse.core;

public interface IFavouriteMetricsListener {

  void metricAdded(String metricKey);

  void metricRemoved(String metricKey);

}
