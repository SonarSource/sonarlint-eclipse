package org.sonar.ide.eclipse.jdt;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.core.SonarLogger;

public class SonarJdtPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.jdt";

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    SonarLogger.log("SonarJdtPlugin started");
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    SonarLogger.log("SonarJdtPlugin stopped");
    super.stop(context);
  }

}
