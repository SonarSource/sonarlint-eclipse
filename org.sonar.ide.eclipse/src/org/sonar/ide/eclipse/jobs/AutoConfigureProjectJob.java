package org.sonar.ide.eclipse.jobs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * Auto configuration of projects by searching the equivalent on the server.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-47
 * 
 * @author Jérémie Lagarde
 * 
 */
public class AutoConfigureProjectJob extends Job {

  private final IProject[] projects;

  public AutoConfigureProjectJob(IProject project) {
    super(project.getName());
    this.projects = new IProject[] { project };
    setPriority(Job.LONG);
  }

  public AutoConfigureProjectJob(IProject[] projects) {
    super("Retrieve project information in sonar server"); // TODO put in
                                                           // messages.properties
    this.projects = projects;
    setPriority(Job.LONG);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus status = null;
    try {
      for (int i = 0; i < projects.length; i++) {
        if (projects[i].isOpen() && !monitor.isCanceled()) {
          retrieveProjectConfiguration(JavaCore.create(projects[i]), monitor);
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

  private void retrieveProjectConfiguration(IJavaProject project, IProgressMonitor monitor) throws Exception {

  }

}