package org.sonar.ide.eclipse.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.jobs.RefreshDuplicationsJob;
import org.sonar.ide.eclipse.jobs.RefreshViolationJob;

public class RefreshAllAction extends AbstractRefreshAction {

  @Override
  protected Job createJob(List<IResource> resources) {
    return null;
  }

  @Override
  protected Job[] createJobs(List<IResource> resources) {
    return new Job[] { new RefreshDuplicationsJob(resources), new RefreshViolationJob(resources) };
  }

}
