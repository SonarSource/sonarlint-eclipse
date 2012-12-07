/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.mylyn.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarTaskSchema;
import org.sonar.ide.eclipse.ui.internal.util.PlatformUtils;

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

        IResource resource = ResourceUtils.getResource(sonarResourceKey);
        if (!(resource instanceof IFile)) {
          MessageDialog.openWarning(null, "Resource not found", "Failed to locate resource '" + sonarResourceKey + "' in workspace.");
        } else {
          PlatformUtils.openEditor((IFile) resource, line);
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
