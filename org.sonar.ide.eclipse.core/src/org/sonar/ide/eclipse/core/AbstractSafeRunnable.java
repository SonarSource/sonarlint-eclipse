package org.sonar.ide.eclipse.core;

import org.eclipse.core.runtime.ISafeRunnable;

public abstract class AbstractSafeRunnable implements ISafeRunnable {
  public void handleException(Throwable exception) {
  }
}
