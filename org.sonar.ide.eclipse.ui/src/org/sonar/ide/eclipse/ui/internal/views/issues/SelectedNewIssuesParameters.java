package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.ui.views.markers.FiltersContributionParameters;

import java.util.HashMap;
import java.util.Map;

public class SelectedNewIssuesParameters extends FiltersContributionParameters {

  private static Map<String, Object> isNewMap;
  static {
    isNewMap = new HashMap<String, Object>();
    isNewMap.put(IsNewIssueFieldFilter.TAG_SELECTED_NEW, IsNewIssueFieldFilter.SHOW_NEW);
  }

  public SelectedNewIssuesParameters() {
    super();
  }

  public Map getParameterValues() {
    return isNewMap;
  }

}
