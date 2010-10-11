package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.resources.IFile;
import org.sonar.ide.eclipse.core.ISonarFile;

public class SonarFile extends SonarResource implements ISonarFile {
  public SonarFile(IFile file, String key) {
    super(file, key);
  }
}
