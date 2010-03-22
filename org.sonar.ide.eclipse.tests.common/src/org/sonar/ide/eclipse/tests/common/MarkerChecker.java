package org.sonar.ide.eclipse.tests.common;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.sonar.ide.api.Logs;

/**
 * This class is use to check Marker.
 * 
 * @author Jérémie Lagarde
 */
class MarkerChecker {
  private long   makerId;
  private int    priority;
  private long   line;
  private String message;

  public MarkerChecker(int priority, long line, String message) {
    this.priority = priority;
    this.line = line;
    this.message = message;
  }

  public boolean check(IMarker marker) {
    try {     
      if (line != Long.parseLong(marker.getAttribute(IMarker.LINE_NUMBER).toString()))
        return false;
      if (priority != Integer.parseInt(marker.getAttribute(IMarker.PRIORITY).toString()))
        return false;
      if (!StringUtils.equals(message, marker.getAttribute(IMarker.MESSAGE).toString()))
        return false;
    } catch (Throwable ex) {
      ex.printStackTrace();
      makerId = 0;
      return false;
    }
    makerId = marker.getId();
    return true;
  }

  public boolean isChecked() {
    return makerId != 0;
  }
}
