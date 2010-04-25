package org.sonar.ide.eclipse.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonar.ide.eclipse.Messages;

/**
 * @author Jérémie Lagarde
 *
 */
public class SonarProjectPropertyBlock {

  private Text serverUrlText;
  private Text projectGroupIdText;
  private Text projectArtifactIdText;
  
  public Control createContents(Composite parent, ProjectProperties projectProperties) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;
    
    addServerData(container, projectProperties);
    addSeparator(container);
    addProjectData(container, projectProperties);

    return container;
  }


  private void addServerData(Composite container, ProjectProperties projectProperties) {   
    // Sonar Server Url
    Label labelUrl = new Label(container, SWT.NULL);
    labelUrl.setText(Messages.getString("pref.project.label.host")); //$NON-NLS-1$
    serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    serverUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    serverUrlText.setText(projectProperties.getUrl());
  }

  private void addSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.horizontalSpan = 2;
    gridData.grabExcessHorizontalSpace = true;
    separator.setLayoutData(gridData);
  }
  


  private void addProjectData(Composite container, ProjectProperties projectProperties) {
    // Project groupId
    Label labelGroupId = new Label(container, SWT.NULL);
    labelGroupId.setText(Messages.getString("pref.project.label.groupid")); //$NON-NLS-1$
    projectGroupIdText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectGroupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectGroupIdText.setText(projectProperties.getGroupId());
    
    // Project artifactId
    Label labelArtifactId = new Label(container, SWT.NULL);
    labelArtifactId.setText(Messages.getString("pref.project.label.artifactid")); //$NON-NLS-1$
    projectArtifactIdText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectArtifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectArtifactIdText.setText(projectProperties.getArtifactId());
  }

  protected String getUrl() {
    return serverUrlText.getText();
  }

  protected String getGroupId() {
    return projectGroupIdText.getText();
  }

  protected String getArtifactId() {
    return projectArtifactIdText.getText();
  }
  
}
