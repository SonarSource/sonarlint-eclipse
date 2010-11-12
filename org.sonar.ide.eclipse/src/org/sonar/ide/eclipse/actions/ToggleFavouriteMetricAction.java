package org.sonar.ide.eclipse.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.core.FavoriteMetricsManager;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.ide.eclipse.utils.PlatformUtils;
import org.sonar.ide.eclipse.utils.SelectionUtils;

public class ToggleFavouriteMetricAction extends BaseSelectionListenerAction {
  public ToggleFavouriteMetricAction() {
    super("");
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    ISonarMetric metric = getSelectedMetric(selection);
    if (metric == null) {
      return false;
    }
    if (FavoriteMetricsManager.getInstance().isFavorite(metric)) {
      setText("Remove from favourites");
      setImageDescriptor(SonarImages.STAR_OFF);
    } else {
      setText("Add to favourites");
      setImageDescriptor(SonarImages.STAR);
    }
    return true;
  };

  @Override
  public void run() {
    ISonarMetric metric = getSelectedMetric(getStructuredSelection());
    FavoriteMetricsManager.getInstance().toggle(metric);
    selectionChanged(getStructuredSelection());
  };

  private ISonarMetric getSelectedMetric(IStructuredSelection selection) {
    Object obj = SelectionUtils.getSingleElement(selection);
    return PlatformUtils.adapt(obj, ISonarMetric.class);
  }
}
