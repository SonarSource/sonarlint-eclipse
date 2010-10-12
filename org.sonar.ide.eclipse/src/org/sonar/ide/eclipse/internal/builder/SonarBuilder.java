package org.sonar.ide.eclipse.internal.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Map;

public class SonarBuilder extends IncrementalProjectBuilder {
  @SuppressWarnings("rawtypes")
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    return null;
  }
}
