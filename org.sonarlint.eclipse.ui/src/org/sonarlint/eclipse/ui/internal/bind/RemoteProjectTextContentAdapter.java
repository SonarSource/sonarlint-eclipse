/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.bind;

import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Control;
import org.sonarlint.eclipse.core.internal.server.RemoteSonarProject;

/**
 * This adapter will update the model ({@link ProjectBindModel}) of a row
 * with what is provided by the content assist. Because content assist can only return
 * a String there is a serialization that is done with {@link RemoteSonarProject#asString()}
 * and here we can deserialize using {@link RemoteSonarProject#fromString(String)}
 *
 */
public class RemoteProjectTextContentAdapter extends TextContentAdapter {
  private ProjectBindModel project;

  public RemoteProjectTextContentAdapter(ProjectBindModel project) {
    this.project = project;
  }

  @Override
  public void insertControlContents(Control control, String text, int cursorPosition) {
    // Don't insert but instead replace
    setControlContents(control, text, cursorPosition);
  }

  @Override
  public void setControlContents(Control control, String text, int cursorPosition) {
    RemoteSonarProject prj = RemoteSonarProject.fromString(text);
    project.associate(prj.getServerId(), prj.getProjectKey());
    super.setControlContents(control, prj.getProjectKey(), cursorPosition);
  }

}
