package org.sonarlint.eclipse.m2e.internal;

import org.eclipse.core.resources.IFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileFilter;

public class MavenModuleFilter implements ISonarLintFileFilter {

  private final boolean isM2ePresent;

  public MavenModuleFilter() {
    this.isM2ePresent = isM2ePresent();
  }

  private static boolean isM2ePresent() {
    try {
      Class.forName("org.eclipse.m2e.core.MavenPlugin");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean test(IFile file) {
    if (isM2ePresent) {
      return !M2eUtils.isInNestedModule(file);
    }
    return true;
  }

}
