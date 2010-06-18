package org.sonar.ide.eclipse.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.sonar.ide.eclipse.console.SonarConsolePreferenceBlock;

/**
 * @author Evgeny Mandrikov
 */
public class SonarConsolePreferencePage extends AbstractSonarPreferencePage {

  private SonarConsolePreferenceBlock consoleBlock;

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    consoleBlock = new SonarConsolePreferenceBlock();
    consoleBlock.createContents(container);
    return container;
  }

  @Override
  protected void performApply() {
    consoleBlock.performApply(getPreferenceStore());
  }

}
