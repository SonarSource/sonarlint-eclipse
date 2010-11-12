package org.sonar.ide.eclipse.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.core.FavoriteMetricsManager;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.utils.SelectionUtils;

public class ToggleFavouriteMetricAction extends BaseSelectionListenerAction {
  public ToggleFavouriteMetricAction() {
    super("");
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    ISonarMeasure measure = getMeasure(selection);
    if (measure == null) {
      return false;
    }
    if (FavoriteMetricsManager.getInstance().isFavorite(measure.getMetricKey())) {
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
    ISonarMeasure measure = getMeasure(getStructuredSelection());
    String metricKey = measure.getMetricKey();
    FavoriteMetricsManager.getInstance().toggle(metricKey);
    selectionChanged(getStructuredSelection());
  };

  private ISonarMeasure getMeasure(IStructuredSelection selection) {
    Object sel = SelectionUtils.getSingleElement(selection);
    if (sel instanceof ISonarMeasure) {
      return (ISonarMeasure) sel;
    }
    return null;
  }
}
