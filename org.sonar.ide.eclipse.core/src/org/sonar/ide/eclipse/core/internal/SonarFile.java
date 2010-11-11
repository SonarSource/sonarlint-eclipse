package org.sonar.ide.eclipse.core.internal;

import org.eclipse.core.resources.IFile;
import org.sonar.ide.eclipse.core.ISonarFile;

public class SonarFile extends SonarResource implements ISonarFile {
  public SonarFile(IFile file, String key, String name) {
    super(file, key, name);
  }
}
