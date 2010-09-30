/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.core.SonarLogger;

/**
 * Property page for projects to configure sonar server connection. It store in
 * <project>/.settings/org.sonar.ide.eclipse.prefs following properties:
 * - url,
 * - groupId, artifactId, branch
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
    if (parent == null) {
      return new Composite(parent, SWT.NULL);
    }
    ProjectProperties projectProperties = ProjectProperties.getInstance(getProject());
    if (projectProperties == null) {
      return new Composite(parent, SWT.NULL);
    }
    block = new SonarProjectPropertyBlock(getProject());
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
    if (projectProperties == null || block == null) {
      return;
    }
    projectProperties.setUrl(block.getUrl());
    projectProperties.setGroupId(block.getGroupId());
    projectProperties.setArtifactId(block.getArtifactId());
    projectProperties.setBranch(block.getBranch());
    try {
      projectProperties.save();
    } catch (SonarIdeException e) {
      SonarLogger.log(e);
    }
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

}
