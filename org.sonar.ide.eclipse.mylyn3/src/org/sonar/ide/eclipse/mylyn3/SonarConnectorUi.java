package org.sonar.ide.eclipse.mylyn3;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;


public class SonarConnectorUi extends AbstractRepositoryConnectorUi {

  @Override
  public String getConnectorKind() {
    // TODO Auto-generated method stub
    return "Sonar Mylyn Kind";
  }

  @Override
  public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping taskSelection) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ITaskRepositoryPage getSettingsPage(TaskRepository repository) {
    // TODO Auto-generated method stub
    return new SonarSettingsPage("title", "description", repository);
  }

  @Override
  public boolean hasSearchPage() {
    // TODO Auto-generated method stub
    return false;
  }

}
