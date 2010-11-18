package org.sonar.ide.eclipse.internal.ui.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.SonarPlugin;
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
    if (SonarPlugin.getFavouriteMetricsManager().isFavorite(metric)) {
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
    SonarPlugin.getFavouriteMetricsManager().toggle(metric);
    selectionChanged(getStructuredSelection());
  };

  private ISonarMetric getSelectedMetric(IStructuredSelection selection) {
    Object obj = SelectionUtils.getSingleElement(selection);
    if (obj == null) {
      return null;
    }
    return PlatformUtils.adapt(obj, ISonarMetric.class);
  }
}
