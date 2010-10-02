package org.sonar.ide.eclipse.internal;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.PlatformUtils;
import org.sonar.ide.wsclient.RemoteSonar;
import org.sonar.wsclient.Host;

import java.io.IOException;

/**
 * This is experimental class, which maybe removed in future.
 * Used for migration to new API.
 * 
 * @author Evgeny Mandrikov
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
   * For Eclipse use {@link #search(ISonarResource)} instead of it. {@inheritDoc}
   */
  @Override
  @Deprecated
  public SourceCode search(String key) {
    return super.search(key);
  }

  /**
   * @return null, if not found
   */
  public SourceCode search(ISonarResource resource) {
    return super.search(resource.getKey());
  }

  private static void displayError(Throwable e) {
    SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
  }

  /**
   * @return null, if not found
   */
  public SourceCode search(IResource resource) {
    ISonarResource element = PlatformUtils.adapt(resource, ISonarResource.class);
    if (element == null) {
      return null;
    }
    String key = element.getKey();

    SourceCode code = search(key);
    if (code == null) {
      return null;
    }

    if (resource instanceof IFile) {
      IFile file = (IFile) resource;
      try {
        String content = IOUtils.toString(file.getContents(), file.getCharset());
        code.setLocalContent(content);
      } catch (CoreException e) {
        displayError(e);
      } catch (IOException e) {
        displayError(e);
      }
    }
    return code;
  }

}
