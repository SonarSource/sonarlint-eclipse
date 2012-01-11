/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.mylyn.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarTaskSchema;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.wsclient.services.Resource;

public class ResourceAttributeEditor extends AbstractAttributeEditor {

  public ResourceAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
    super(manager, taskAttribute);
    setLayoutHint(new LayoutHint(RowSpan.SINGLE, ColumnSpan.MULTIPLE));
  }

  @Override
  public void createControl(Composite parent, FormToolkit toolkit) {
    Hyperlink hyperlink = toolkit.createHyperlink(parent, getResource(), SWT.NONE);
    hyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        String sonarResourceKey = getResource();
        Integer line = getLine();

        // Godin: in fact this is not a good way to locate IFile by Sonar resourceKey and should be improved
        IFile file = PlatformUtils.adapt(new Resource().setKey(sonarResourceKey), IFile.class);
        if (file == null) {
          MessageDialog.openWarning(null, "Resource not found", "Failed to locate resource '" + sonarResourceKey + "' in workspace.");
        } else {
          PlatformUtils.openEditor(file, line);
        }
      }
    });
    setControl(hyperlink);
  }

  private String getResource() {
    return getTaskAttribute().getValue();
  }

  private Integer getLine() {
    TaskAttribute attribute = getTaskAttribute().getParentAttribute().getAttribute(SonarTaskSchema.getDefault().LINE.getKey());
    return getAttributeMapper().getIntegerValue(attribute);
  }

}
