/*
 * Copyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.coverage.CoverageData;
import org.sonar.ide.shared.coverage.CoverageLoader;
import org.sonar.wsclient.Sonar;

/**
 * This class load code coverage in background.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-60
 * 
 * @author Jérémie Lagarde
 * 
 */
public class RefreshCoverageJob extends AbstractRefreshModelJob<CoverageData> {


  public RefreshCoverageJob(final List<IResource> resources) {
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
      monitor.beginTask("Retrieve sonar coverage for " + unit.getElementName(), 1);
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(unit.getResource());
      final Map<Integer, String> coverage = CoverageLoader.getCoverageLineHits(sonar, resourceKey);
      for (final Integer line : coverage.keySet()) {
        // create a marker for thcoveragee actual resource
        creatMarker(unit, line, coverage.get(line));
      }
    } catch (final Exception ex) {
      // TODO : best exception management.
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  private IMarker creatMarker(final ICompilationUnit unit, final Integer line, final String code) throws CoreException {
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    if ("0".equals(code)) {
      markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
    }
    if ("1".equals(code)) {
      markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_INFO));
    }
    if ("2".equals(code)) {
      markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
    }
    markerAttributes.put(IMarker.LINE_NUMBER, line);
    markerAttributes.put(IMarker.MESSAGE, "Code coverage  :" + code);
    addLine(markerAttributes, line, unit.getSource());
    final IMarker marker = unit.getResource().createMarker("org.sonar.ide.eclipse.sonarCoverageMarker");
    marker.setAttributes(markerAttributes);
    return marker;
  }
}