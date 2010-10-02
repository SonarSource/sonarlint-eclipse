package org.sonar.ide.eclipse.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.ui.AbstractSonarInfoView;

/**
 * @author Evgeny Mandrikov
 */
public class RemoteView extends AbstractSonarInfoView {

  public static final String ID = "org.sonar.ide.eclipse.views.RemoteView";

  private Browser browser;

  @Override
  protected Control getControl() {
    return browser;
  }

  @Override
  protected void internalCreatePartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
    clear();
  }

  /**
   * @param input ISonarResource to be showin in the view
   */
  @Override
  protected void doSetInput(Object input) {
    ISonarResource sonarResource = (ISonarResource) input;
    SourceCode sourceCode = EclipseSonar.getInstance(sonarResource.getProject()).search(sonarResource);
    if (sourceCode == null) {
      browser.setText("Not found.");
      return;
    }
    ProjectProperties properties = ProjectProperties.getInstance(sonarResource.getProject());
    StringBuffer url = new StringBuffer(properties.getUrl()).append("/resource/index/").append(sourceCode.getKey());
    if (sonarResource.getResource() instanceof IFile) {
      url.append("?metric=coverage");
    } else {
      url.append("?page=dashboard");
    }
    browser.setUrl(url.toString());
  }

  private void clear() {
    browser.setText("Select Java project, package or class in Package Explorer.");
  }
}
