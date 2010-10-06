package org.sonar.ide.eclipse.utils;

import org.apache.commons.lang.StringUtils;

public class SonarKeyUtils {

  public static final String MAIN_DELIMITER = ":";

  public static String getKeyForProject(String groupId, String artifactId, String branch) {
    StringBuilder sb = new StringBuilder().append(groupId).append(MAIN_DELIMITER).append(artifactId);
    if (StringUtils.isNotBlank(branch)) {
      sb.append(MAIN_DELIMITER).append(branch);
    }
    return sb.toString();
  }

  private SonarKeyUtils() {
  }

}
