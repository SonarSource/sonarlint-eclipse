package org.sonar.ide.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * Property page for projects to configure sonar server connection. It store in
 * <project>/.settings/org.sonar.ide.eclipse.prefs following properties:
 *   - url, username, password
 *   - groupId, artifactId
 * 
 * @author Jérémie Lagarde
 */
public class SonarProjectPropertyPage extends PropertyPage {

  private SonarProjectPropertyBlock block;

  public SonarProjectPropertyPage() {
    setTitle(Messages.getString("pref.project.title"));
  }

  @Override
  protected Control createContents(Composite parent) {
    if (parent == null)
      return new Composite(parent, SWT.NULL);
    ProjectProperties projectProperties = ProjectProperties.getInstance(getProject());
    if (projectProperties == null)
      return new Composite(parent, SWT.NULL);
    block = new SonarProjectPropertyBlock();
    return block.createContents(parent, projectProperties);
  }

  @Override
  public boolean performOk() {
    performApply();
    return super.performOk();
  }

  @Override
  protected void performApply() {
    ProjectProperties projectProperties = ProjectProperties.getInstance(getProject());
    if (projectProperties == null || block ==null)
      return;
    projectProperties.setUrl(block.getUrl());
    projectProperties.setUsername(block.getUsername());
    projectProperties.setPassword(block.getPassword());
    projectProperties.setGroupId(block.getGroupId());
    projectProperties.setArtifactId(block.getArtifactId());
    try {
      projectProperties.flush();
    } catch (BackingStoreException e) {
      SonarPlugin.getDefault().writeLog(IStatus.ERROR, e.getMessage(), e);
    }
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

}
