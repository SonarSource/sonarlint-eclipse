package org.sonar.ide.eclipse.ui.its.utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CaptureScreenshotOnFailure implements TestRule {
  @Override
  public final Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } catch (Throwable onHold) {
          String fileName = constructFilename(description);
          new SWTWorkbenchBot().captureScreenshot(fileName);
          throw onHold;
        }
      }

      private String constructFilename(final Description description) {
        return "./target/"
          + description.getClassName() + "."
          + description.getMethodName() + ".png";
      }
    };
  }

}
