package org.sonar.ide.eclipse.internal.ui.jobs;

import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.internal.core.ISonarConstants;

public abstract class AbstractRemoteSonarJob extends Job {
  public AbstractRemoteSonarJob(String name) {
    super(name);
  }

  @Override
  public boolean belongsTo(Object family) {
    return family.equals(ISonarConstants.REMOTE_SONAR_JOB_FAMILY) ? true : super.belongsTo(family);
  }
}
