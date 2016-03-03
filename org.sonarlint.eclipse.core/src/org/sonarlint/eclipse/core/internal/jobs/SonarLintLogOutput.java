package org.sonarlint.eclipse.core.internal.jobs;

import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarsource.sonarlint.core.client.api.LogOutput;

public final class SonarLintLogOutput implements LogOutput {
  @Override
  public void log(String msg, Level level) {
    switch (level) {
      case TRACE:
      case DEBUG:
        SonarLintCorePlugin.getDefault().debug(msg);
        break;
      case INFO:
      case WARN:
        SonarLintCorePlugin.getDefault().info(msg);
        break;
      case ERROR:
        SonarLintCorePlugin.getDefault().error(msg);
        break;
      default:
        SonarLintCorePlugin.getDefault().info(msg);
    }

  }
}
