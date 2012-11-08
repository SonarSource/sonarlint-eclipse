package org.sonar.ide.eclipse.internal.ui.wizards.associate;

import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Control;

/**
 * This adapter will update the model ({@link ProjectAssociationModel}) of a row
 * with what is provided by the content assist. Because content assist can only return
 * a String there is a serialization that is done with {@link RemoteSonarProject#asString()}
 * and here we can deserialize using {@link RemoteSonarProject#fromString(String)}
 * @author julien
 *
 */
public class RemoteSonarProjectTextContentAdapter extends TextContentAdapter {
  private ProjectAssociationModel sonarProject;

  public RemoteSonarProjectTextContentAdapter(ProjectAssociationModel sonarProject) {
    this.sonarProject = sonarProject;
  }

  @Override
  public String getControlContents(Control control) {
    return super.getControlContents(control);
  }

  @Override
  public void insertControlContents(Control control, String text, int cursorPosition) {
    RemoteSonarProject prj = RemoteSonarProject.fromString(text);
    sonarProject.associate(prj.getUrl(), prj.getName(), prj.getKey());
    // Don't insert but instead replace
    super.setControlContents(control, prj.getName(), cursorPosition);
  }

  @Override
  public void setControlContents(Control control, String text, int cursorPosition) {
    RemoteSonarProject prj = RemoteSonarProject.fromString(text);
    sonarProject.associate(prj.getUrl(), prj.getName(), prj.getKey());
    super.setControlContents(control, prj.getName(), cursorPosition);
  }

}
