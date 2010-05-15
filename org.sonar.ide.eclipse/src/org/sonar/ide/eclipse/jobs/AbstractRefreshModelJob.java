/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Model;

/**
 * @author Jérémie Lagarde
 */
public abstract class AbstractRefreshModelJob<M extends Model> extends Job implements IResourceVisitor {

  private final List<IResource>      resources;
  private final String               markerId;
  private IProgressMonitor           monitor;
  private IStatus                    status;
  private final Map<IProject, Sonar> sonars = new HashMap<IProject, Sonar>();

  public AbstractRefreshModelJob(final List<IResource> resources, final String markerId) {
    super("Retrieve sonar " + markerId);
    setPriority(Job.LONG);
    this.resources = resources;
    this.markerId = markerId;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    this.monitor = monitor;
    try {
      // TODO put it in messages.properties
      monitor.beginTask("Retrieve sonar data", resources.size());

      for (final IResource resource : resources) {
        if (!monitor.isCanceled()) {
          resource.accept(this);
        }
      }
      if (!monitor.isCanceled()) {
        status = Status.OK_STATUS;
      } else {
        status = Status.CANCEL_STATUS;
      }
    } catch (final Exception e) {
      status = new Status(IStatus.ERROR, SonarPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e);
    } finally {
      monitor.done();
    }
    return status;
  }

  public boolean visit(final IResource resource) throws CoreException {
    if (resource instanceof IFile) {
      final IJavaElement element = JavaCore.create((IFile) resource);
      if (element instanceof ICompilationUnit) {
        final ICompilationUnit unit = (ICompilationUnit) element;
        cleanMarkers(unit);
        retrieveMarkers(unit, monitor);
      }
    }
    return true;
  }

  protected Sonar getSonar(final IProject project) {
    if (sonars.containsKey(project)) {
      return sonars.get(project);
    }
    final ProjectProperties properties = ProjectProperties.getInstance(project);
    final Sonar sonar = SonarPlugin.getServerManager().getSonar(properties.getUrl());
    sonars.put(project, sonar);
    return sonar;
  }

  abstract protected void retrieveMarkers(ICompilationUnit unit, IProgressMonitor monitor) throws CoreException;

  /**
   * Remove all sonar markers
   * 
   * @param project
   *          The project to clean
   * @throws CoreException
   */
  private void cleanMarkers(final ICompilationUnit unit) throws CoreException {
    unit.getResource().deleteMarkers(markerId, true, IResource.DEPTH_ZERO);
  }

  // TODO : need to refactor it.
  protected void addLine(final Map<String, Object> markerAttributes, final long line, final String text) {
    int start = 0;
    for (int i = 1; i < line; i++) {
      start = StringUtils.indexOf(text, '\n', start) + 1;
    }
    final int end = StringUtils.indexOf(text, '\n', start);
    markerAttributes.put(IMarker.CHAR_START, start);
    markerAttributes.put(IMarker.CHAR_END, end);
  }

}
