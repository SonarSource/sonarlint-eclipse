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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.sonar.ide.api.Logs;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.ViolationUtils;
import org.sonar.ide.shared.ViolationsLoader;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Violation;

/**
 * This class load violations in background.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 * 
 * @author Jérémie Lagarde
 * 
 */
public class RefreshViolationJob extends AbstractRefreshModelJob<Violation> {


  public RefreshViolationJob(final List<IResource> resources) {
    super(resources, SonarPlugin.MARKER_ID);
  }

  @Override
  protected void retrieveMarkers(final ICompilationUnit unit, final IProgressMonitor monitor) throws CoreException {
    if (unit == null || !unit.exists() || monitor.isCanceled()) {
      return;
    }

    final Sonar sonar = getSonar(unit.getResource().getProject());

    try {
      // TODO put it in messages.properties
      monitor.beginTask("Retrieve sonar violations for " + unit.getElementName(), 1);
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(unit.getResource());
      final Collection<Violation> violations = ViolationsLoader.getViolations(sonar, resourceKey, unit.getSource());
      for (final Violation violation : violations) {
        // create a marker for the actual resource
        creatMarker(unit, violation);
      }
    } catch (final Exception ex) {
      // TODO : best exception management.
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  private IMarker creatMarker(final ICompilationUnit unit, final Violation violation) throws CoreException {
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    Logs.INFO.debug("Create marker : " + violation.getPriority());
    if (ViolationUtils.PRIORITY_BLOCKER.equalsIgnoreCase(violation.getPriority())) {
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
    }
    if (ViolationUtils.PRIORITY_CRITICAL.equalsIgnoreCase(violation.getPriority())) {
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
    }
    if (ViolationUtils.PRIORITY_MAJOR.equalsIgnoreCase(violation.getPriority())) {
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_NORMAL));
    }
    if (ViolationUtils.PRIORITY_MINOR.equalsIgnoreCase(violation.getPriority())) {
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_LOW));
    }
    if (ViolationUtils.PRIORITY_INFO.equalsIgnoreCase(violation.getPriority())) {
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_LOW));
    }
    markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
    markerAttributes.put(IMarker.LINE_NUMBER, violation.getLine());
    markerAttributes.put(IMarker.MESSAGE, ViolationUtils.getDescription(violation));
    markerAttributes.put("rulekey", violation.getRuleKey());
    markerAttributes.put("rulename", violation.getRuleName());
    addLine(markerAttributes, violation.getLine(), unit.getSource());
    final IMarker marker = unit.getResource().createMarker(SonarPlugin.MARKER_ID);
    marker.setAttributes(markerAttributes);
    return marker;
  }

}