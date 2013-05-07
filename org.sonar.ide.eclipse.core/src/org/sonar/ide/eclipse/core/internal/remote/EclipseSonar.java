/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.remote;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;

import java.io.IOException;

/**
 * This is experimental class, which maybe removed in future. Used for migration to new API.
 *
 * @author Evgeny Mandrikov
 */
public final class EclipseSonar {

  public static EclipseSonar getInstance(IProject project) {
    SonarProject sonarProject = SonarProject.getInstance(project);
    ISonarServer sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (sonarServer == null) {
      throw new SonarEclipseException(NLS.bind(Messages.No_matching_server_in_configuration_for_project,
          sonarProject.getProject().getName(), sonarProject.getUrl()));
    }
    return new EclipseSonar(sonarServer);
  }

  private final RemoteSonarIndex index;
  private ISonarServer sonarServer;

  /**
   * It's better to use {@link #getInstance(IProject)} instead of it.
   */
  public EclipseSonar(ISonarServer sonarServer) {
    this.sonarServer = sonarServer;
    index = new RemoteSonarIndex(sonarServer, new SimpleSourceCodeDiffEngine());
  }

  public SourceCode search(String key) {
    return index.search(key);
  }

  /**
   * @return null, if not found
   */
  public SourceCode search(ISonarResource resource) {
    return search(resource.getKey());
  }

  private static void displayError(Throwable e) {
    LoggerFactory.getLogger(EclipseSonar.class).error(e.getMessage(), e);
  }

  /**
   * @TODO Godin: maybe it would be better to return special object, which will represent non-existing resource
   * @return null, if not found
   */
  public SourceCode search(IResource resource) {
    ISonarResource element = ResourceUtils.adapt(resource);
    if (element == null) {
      return null;
    }
    SourceCode code = search(element);
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

  public ISonarServer getSonarServer() {
    return sonarServer;
  }
}
