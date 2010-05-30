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
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.wsclient.Sonar;
import org.sonar.ide.shared.duplications.Duplication;
import org.sonar.ide.shared.duplications.DuplicationsLoader;

/**
 * This class load duplications in background.
 * 
 * @author Evgeny Mandrikov
 */
public class RefreshDuplicationsJob extends AbstractRefreshModelJob<Duplication> {

  public RefreshDuplicationsJob(final List<IResource> resources) {
    super(resources, SonarPlugin.MARKER_ID);
  }

  @Override
  protected void retrieveMarkers(ICompilationUnit unit, IProgressMonitor monitor) throws CoreException {
    if (unit == null || !unit.exists() || monitor.isCanceled()) {
      return;
    }

    final Sonar sonar = getSonar(unit.getResource().getProject());

    try {
      // TODO put it in messages.properties
      monitor.beginTask("Retrieve sonar duplications for " + unit.getElementName(), 1);
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(unit.getResource());
      final Collection<Duplication> duplications = DuplicationsLoader.getDuplications(sonar, resourceKey, unit.getSource());
      for (final Duplication duplication : duplications) {
        // create a marker for the actual resource
        creatMarker(unit, duplication);
      }
    } catch (final Exception ex) {
      // TODO : best exception management.
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  private IMarker creatMarker(final ICompilationUnit unit, final Duplication duplication) throws CoreException {
    // TODO Godin : improve message and so on
    final Map<String, Object> markerAttributes = new HashMap<String, Object>();
    markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
    markerAttributes.put(IMarker.LINE_NUMBER, duplication.getStart());
    markerAttributes.put(IMarker.MESSAGE, "Duplicates code from " + duplication.getTargetResource() + ":" + duplication.getTargetStart());
    addLine(markerAttributes, duplication.getStart(), unit.getSource());
    final IMarker marker = unit.getResource().createMarker(SonarPlugin.MARKER_ID);
    marker.setAttributes(markerAttributes);
    return marker;
  }
}
