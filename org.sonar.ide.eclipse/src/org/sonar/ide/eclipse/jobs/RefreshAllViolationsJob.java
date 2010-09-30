package org.sonar.ide.eclipse.jobs;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.sonar.ide.api.Logs;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.wsclient.services.Violation;

import com.google.common.collect.ArrayListMultimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshAllViolationsJob extends RefreshViolationsJob {

  public static void createAndSchedule() {
    List<IResource> resources = new ArrayList<IResource>();
    Collections.addAll(resources, ResourcesPlugin.getWorkspace().getRoot().getProjects());
    new RefreshAllViolationsJob(resources).schedule();
  }

  protected RefreshAllViolationsJob(List<IResource> resources) {
    super(resources);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // TODO Godin: remove before commit - it's just for performance testing
    long time = System.currentTimeMillis();
    IStatus status = super.run(monitor);
    time = System.currentTimeMillis() - time;
    Logs.INFO.info("Loaded in {}ms = {}s", time, time / 1000);
    return status;
  }

  @Override
  public boolean visit(final IResource resource) throws CoreException {
    if (resource instanceof IProject) {
      IProject project = (IProject) resource;
      // We will work only with Java projects
      if ( !project.hasNature(JavaCore.NATURE_ID)) {
        return false;
      }

      EclipseSonar sonar = EclipseSonar.getInstance(project);
      SourceCode sourceCode = sonar.search(project);
      if (sourceCode != null) {
        List<Violation> violations = sourceCode.getViolations2();
        // Split violations by resource
        ArrayListMultimap<String, Violation> mm = ArrayListMultimap.create();
        for (Violation violation : violations) {
          if (violation.getLine() != null) { // TODO violation not associated with line
            mm.put(violation.getResourceKey(), violation);
          }
        }
        // Associate violations with resources
        IJavaProject javaProject = JavaCore.create(project);
        for (String resourceKey : mm.keySet()) {
          String className = StringUtils.substringAfterLast(resourceKey, ":");
          IType type = javaProject.findType(className);

          if (type != null) {
            ICompilationUnit unit = type.getCompilationUnit();
            if (unit != null) {
              cleanMarkers((IFile) type.getResource());
              for (Violation violation : mm.get(resourceKey)) {
                createMarker((IFile) unit.getResource(), violation);
              }
            }
          }
        }
      }
      return false; // do not visit members of this resource
    }
    return true;
  }
}
