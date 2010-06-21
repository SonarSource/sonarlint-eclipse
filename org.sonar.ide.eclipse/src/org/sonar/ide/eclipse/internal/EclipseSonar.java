package org.sonar.ide.eclipse.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.wsclient.RemoteSonar;
import org.sonar.wsclient.Host;

/**
 * This is experimental class, which maybe removed in future.
 * Used for migration to new API.
 * 
 * @author Evgeny Mandrikov
 * @see #search(ICompilationUnit)
 * @see #search(String, ICompilationUnit)
 */
public final class EclipseSonar extends RemoteSonar {

  public static EclipseSonar getInstance(IProject project) {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    Host host = SonarPlugin.getServerManager().createServer(properties.getUrl());
    return new EclipseSonar(host);
  }

  /**
   * It's better to use {@link #getInstance(IProject)} instead of it.
   */
  public EclipseSonar(Host host) {
    super(host);
  }

  /**
   * For Eclipse use {@link #search(ICompilationUnit)} or {@link #search(String, ICompilationUnit)} instead of it. {@inheritDoc}
   */
  @Override
  public SourceCode search(String key) {
    return super.search(key);
  }

  private SourceCode search(String key, ICompilationUnit unit) {
    SourceCode code = search(key);
    try {
      code.setLocalContent(unit.getSource());
    } catch (JavaModelException e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
    return code;
  }

  public SourceCode search(ICompilationUnit unit) {
    return search(EclipseResourceUtils.getInstance().getFileKey(unit.getResource()), unit);
  }

}
