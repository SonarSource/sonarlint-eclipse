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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.sonar.ide.eclipse.internal.EclipseSonar;

/**
 * @author Jérémie Lagarde
 */
public abstract class AbstractRefreshModelJob<M> extends Job implements IResourceVisitor {

  private final List<IResource> resources;
  private final String markerId;
  private IProgressMonitor monitor;
  private IStatus status;

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
        if ( !monitor.isCanceled() && resource.isAccessible()) {
          resource.accept(this);
        }
        monitor.worked(1);
      }

      if ( !monitor.isCanceled()) {
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
        cleanMarkers(resource);
        retrieveMarkers(unit, monitor);
      }
      return false; // do not visit members of this resource
    }
    return true;
  }

  protected abstract Collection<M> retrieveDatas(EclipseSonar sonar, IResource resource);

  private void retrieveMarkers(final ICompilationUnit unit, final IProgressMonitor monitor) throws CoreException {
    if (unit == null || !unit.exists() || monitor.isCanceled()) {
      return;
    }
    try {
      // TODO put it in messages.properties
      monitor.beginTask("Retrieve sonar informations for " + unit.getElementName(), 1);
      final Collection<M> datas = retrieveDatas(EclipseSonar.getInstance(unit.getResource().getProject()), unit.getResource());
      for (final M data : datas) {
        // create a marker for the actual resource
        creatMarker(unit, data);
      }
    } catch (final Exception ex) {
      // TODO : best exception management.
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  private IMarker creatMarker(final ICompilationUnit unit, final M data) throws CoreException {
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    markerAttributes.put(IMarker.PRIORITY, getPriority(data));
    markerAttributes.put(IMarker.SEVERITY, getSeverity(data));
    markerAttributes.put(IMarker.LINE_NUMBER, getLine(data));
    markerAttributes.put(IMarker.MESSAGE, getMessage(data));
    addLine(markerAttributes, getLine(data), unit.getSource());
    final Map<String, Object> extraInfos = getExtraInfos(data);
    if (extraInfos != null) {
      for (final String key : extraInfos.keySet()) {
        markerAttributes.put(key, extraInfos.get(key));
      }
    }
    final IMarker marker = unit.getResource().createMarker(markerId);
    marker.setAttributes(markerAttributes);
    return marker;
  }

  /**
   * @return The line number.
   */
  protected abstract Integer getLine(M data);

  /**
   * @return Severity marker attribute. A number from the set of error, warning and info severities defined by the platform.
   * 
   * @see IMarker.SEVERITY_ERROR
   * @see IMarker.SEVERITY_WARNING
   * @see IMarker.SEVERITY_INFO
   */
  protected abstract Integer getSeverity(M data);

  /**
   * @return Priority marker attribute. A number from the set of high, normal and low priorities defined by the platform.
   * 
   * @see IMarker.PRIORITY_HIGH
   * @see IMarker.PRIORITY_NORMAL
   * @see IMarker.PRIORITY_LOW
   */
  protected abstract Integer getPriority(M data);

  protected abstract String getMessage(M data);

  protected Map<String, Object> getExtraInfos(final M data) {
    return null;
  }

  /**
   * Remove all Sonar markers.
   */
  private void cleanMarkers(final IResource file) throws CoreException {
    file.deleteMarkers(markerId, true, IResource.DEPTH_ZERO);
  }

  // TODO : need to refactor it.
  private void addLine(final Map<String, Object> markerAttributes, final long line, final String text) {
    int start = 0;
    for (int i = 1; i < line; i++) {
      start = StringUtils.indexOf(text, '\n', start) + 1;
    }
    final int end = StringUtils.indexOf(text, '\n', start);
    markerAttributes.put(IMarker.CHAR_START, start);
    markerAttributes.put(IMarker.CHAR_END, end);
  }

}
