/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.ui.properties.ProjectProperties;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.ide.wsclient.RemoteSonar;
import org.sonar.wsclient.Host;

/**
 * This is experimental class, which maybe removed in future. Used for migration to new API.
 * 
 * @author Evgeny Mandrikov
 */
public final class EclipseSonar extends RemoteSonar {

  public static EclipseSonar getInstance(IProject project) {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    Host host = SonarCorePlugin.getServersManager().findServer(properties.getUrl());
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
    SonarUiPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
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
