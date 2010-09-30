package org.sonar.ide.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class SonarLogger {

  private static ILog LOG;

  public static void setLog(ILog log) {
    LOG = log;
  }

  public static void log(IStatus status) {
    LOG.log(status);
  }

  public static void log(CoreException e) {
    IStatus status = e.getStatus();
    log(status);
  }

  public static void log(Throwable t) {
    log(t.getMessage(), t);
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, msg, t));
  }

  public static void log(String msg) {
    log(new Status(IStatus.OK, ISonarConstants.PLUGIN_ID, msg));
  }

}
