package org.sonar.ide.eclipse.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.api.Logs;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
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
public class RefreshViolationJob extends Job {

  private final IProject[] projects;

  public RefreshViolationJob(IProject project) {
    super(project.getName());
    this.projects = new IProject[] { project };
    setPriority(Job.LONG);
  }

  public RefreshViolationJob(IProject[] projects) {
    super("Retrieve sonar violation"); // TODO put in messages.properties
    this.projects = projects;
    setPriority(Job.LONG);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus status = null;
    try {
      for (int i = 0; i < projects.length; i++) {
        if (projects[i].isOpen() && !monitor.isCanceled()) {
          cleanMarkers(projects[i]);
          retrieveMarkers(JavaCore.create(projects[i]), monitor);
        }
      }
      if (!monitor.isCanceled())
        status = Status.OK_STATUS;
      else
        status = Status.CANCEL_STATUS;
    } catch (Exception e) {
      status = new Status(IStatus.ERROR, SonarPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e);
    } finally {
      monitor.done();
    }
    return status;
  }

  /**
   * Remove all sonar markers
   * 
   * @param project
   *          The project to clean
   * @throws CoreException
   */
  private void cleanMarkers(IProject project) throws CoreException {
    project.deleteMarkers(SonarPlugin.MARKER_ID, false, IResource.DEPTH_INFINITE);
  }

  private void retrieveMarkers(IJavaProject project, IProgressMonitor monitor) throws Exception {
    final ProjectProperties properties = ProjectProperties.getInstance(project.getResource());
    final Sonar sonar = SonarPlugin.getServerManager().getSonar(properties.getUrl());
    List<ICompilationUnit> unitList = getICompilationUnits(project);
    try {
      monitor.beginTask("Retrieve sonar violations for " + project.getElementName(), unitList.size()); // TODO
                                                                                                       // put
                                                                                                       // it
                                                                                                       // in
                                                                                                       // messages.properties
      for (ICompilationUnit unit : unitList) {
        if (monitor.isCanceled())
          break;
        retrieveMarkers(sonar, unit, monitor);
      }
    } finally {
      monitor.done();
    }

  }

  private void retrieveMarkers(Sonar sonar, ICompilationUnit unit, IProgressMonitor monitor) throws Exception {
    try {
      monitor.beginTask("Retrieve sonar violations for " + unit.getElementName(), 1); // TODO
                                                                                      // put
                                                                                      // it
                                                                                      // in
                                                                                      // messages.properties
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(unit.getResource());
      final Collection<Violation> violations = ViolationsLoader.getViolations(sonar, resourceKey, unit.getSource());
      for (Violation violation : violations) {
        // create a marker for the actual resource
        creatMarker(unit, violation);
      }
    } catch (Exception ex) {
      // TODO : best exception management.
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  private IMarker creatMarker(ICompilationUnit unit, Violation violation) throws CoreException {
    Map<String, Object> markerAttributes = new HashMap<String, Object>();
    Logs.INFO.debug("Create marker : " + violation.getPriority());
    if (ViolationUtils.PRIORITY_BLOCKER.equalsIgnoreCase(violation.getPriority()))
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
    if (ViolationUtils.PRIORITY_CRITICAL.equalsIgnoreCase(violation.getPriority()))
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
    if (ViolationUtils.PRIORITY_MAJOR.equalsIgnoreCase(violation.getPriority()))
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_NORMAL));
    if (ViolationUtils.PRIORITY_MINOR.equalsIgnoreCase(violation.getPriority()))
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_LOW));
    if (ViolationUtils.PRIORITY_INFO.equalsIgnoreCase(violation.getPriority()))
      markerAttributes.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_LOW));
    markerAttributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
    markerAttributes.put(IMarker.LINE_NUMBER, violation.getLine());
    markerAttributes.put(IMarker.MESSAGE, ViolationUtils.getDescription(violation));
    markerAttributes.put("rulename", violation.getRuleName());
    markerAttributes.put("priority", violation.getPriority());
    addLine(markerAttributes, violation.getLine(), unit.getSource());
    IMarker marker = unit.getResource().createMarker(SonarPlugin.MARKER_ID);
    marker.setAttributes(markerAttributes);
    return marker;
  }

  /**
   * @param javaProject
   * @return all ICompilationUnit for the project.
   */
  private List<ICompilationUnit> getICompilationUnits(IJavaProject javaProject) {
    List<ICompilationUnit> unitList = new ArrayList<ICompilationUnit>();
    try {
      IPackageFragmentRoot roots[] = javaProject.getPackageFragmentRoots();
      for (int i = 0; i < roots.length; i++) {
        IPackageFragmentRoot packageFragmentRoot = roots[i];
        if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
          IJavaElement childs[] = packageFragmentRoot.getChildren();
          for (int j = 0; j < childs.length; j++) {
            IJavaElement javaElement = childs[j];
            if (javaElement instanceof IPackageFragment) {
              IPackageFragment frag = (IPackageFragment) javaElement;
              ICompilationUnit[] units = frag.getCompilationUnits();
              for (int k = 0; k < units.length; k++) {
                ICompilationUnit compilationUnit = units[k];
                unitList.add(compilationUnit);
              }
            } else if (javaElement instanceof ICompilationUnit) {
              unitList.add((ICompilationUnit) javaElement);
            }
          }
        }
      }
    } catch (JavaModelException e) {
      // TODO
      throw new RuntimeException(e);
    }
    return unitList;
  }

  // TODO : need to refactor it.
  private void addLine(Map<String, Object> markerAttributes, long line, String text) {
    int start = 0;
    for (int i = 1; i < line; i++) {
      start = StringUtils.indexOf(text, '\n', start) + 1;
    }
    int end = StringUtils.indexOf(text, '\n', start);
    markerAttributes.put(IMarker.CHAR_START, start);
    markerAttributes.put(IMarker.CHAR_END, end);
  }

}