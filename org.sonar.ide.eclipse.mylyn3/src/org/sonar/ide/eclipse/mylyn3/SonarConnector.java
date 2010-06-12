package org.sonar.ide.eclipse.mylyn3;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

public class SonarConnector extends AbstractRepositoryConnector {

  @Override
  public boolean canCreateNewTask(TaskRepository repository) {
    return false;
  }

  @Override
  public boolean canCreateTaskFromKey(TaskRepository repository) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getConnectorKind() {
    // TODO Auto-generated method stub
    return "Sonar Mylyn Kind";
  }

  @Override
  public String getLabel() {
    return "Sonar";
  }

  @Override
  public String getRepositoryUrlFromTaskUrl(String url) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getTaskIdFromTaskUrl(String url) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getTaskUrl(String repositoryUrl, String taskId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasTaskChanged(TaskRepository arg0, ITask task, TaskData taskData) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public IStatus performQuery(TaskRepository repository, IRepositoryQuery repositoryQuery, TaskDataCollector resultCollector,
      ISynchronizationSession session, IProgressMonitor monitor) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method stub
  }

  @Override
  public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {
    // TODO Auto-generated method stub
  }

}
