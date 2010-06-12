package org.sonar.ide.eclipse.mylyn3;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractTaskRepositoryPage;
import org.eclipse.swt.widgets.Composite;

public class SonarSettingsPage extends AbstractTaskRepositoryPage {

  public SonarSettingsPage(String title, String description, TaskRepository taskRepository) {
    super(title, description, taskRepository);
    // TODO Auto-generated constructor stub
  }

  @Override
  public String getConnectorKind() {
    // TODO Auto-generated method stub
    return "Sonar Mylyn Kind";
  }

  @Override
  protected void createSettingControls(Composite parent) {
    // TODO Auto-generated method stub
  }

  @Override
  protected IStatus validate() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getRepositoryUrl() {
    // TODO Auto-generated method stub
    return null;
  }

}
