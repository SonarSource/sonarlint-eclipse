package org.sonar.ide.eclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.wsclient.RemoteSonar;
import org.sonar.wsclient.Sonar;

public class EclipseSonar extends RemoteSonar {

  public EclipseSonar(Sonar sonar) {
    super(sonar);
  }

  /**
   * For Eclipse use {@link #search(ICompilationUnit)} instead of it. {@inheritDoc}
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
