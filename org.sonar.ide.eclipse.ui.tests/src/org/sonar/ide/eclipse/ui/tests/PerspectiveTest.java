package org.sonar.ide.eclipse.ui.tests;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IPageLayout;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarUI;
import org.sonar.ide.eclipse.views.MeasuresView;
import org.sonar.ide.eclipse.views.NavigatorView;

/**
 * @author Evgeny Mandrikov
 */
public class PerspectiveTest extends UITestCase {

  @Test
  public void allViewsArePresent() {
    openPerspective(SonarUI.ID_PERSPECTIVE);
    
    bot.viewById(JavaUI.ID_PACKAGES);
    
    bot.viewById(MeasuresView.ID);
    bot.viewById(NavigatorView.ID);

    bot.viewById(IPageLayout.ID_PROBLEM_VIEW);
  }

}
