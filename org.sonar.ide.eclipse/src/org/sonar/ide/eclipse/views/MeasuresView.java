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

package org.sonar.ide.eclipse.views;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.EclipseSonar;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.measures.MeasureData;
import org.sonar.wsclient.Sonar;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends ViewPart {

  public static final String ID = "org.sonar.ide.eclipse.views.MeasuresView";

  private TableViewer viewer;
  private Action linkToEditorAction;
  private boolean linking;

  private void createColumns(final TableViewer viewer) {
    final int[] bounds = { 100, 100, 100 };
    for (int i = 0; i < MeasuresLabelProvider.COLUMNS.length; i++) {
      final TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
      column.getColumn().setText(MeasuresLabelProvider.COLUMNS[i]);
      column.getColumn().setWidth(bounds[i]);
      column.getColumn().setResizable(true);
      column.getColumn().setMoveable(true);
    }
    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
  }

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    createColumns(viewer);
    viewer.setContentProvider(new MeasuresContentProvider());
    viewer.setLabelProvider(new MeasuresLabelProvider());

    // Create actions
    linkToEditorAction = new Action(Messages.getString("action.link"), IAction.AS_CHECK_BOX) {

      @Override
      public void run() {
        toggleLinking(isChecked());
      }
    };
    linkToEditorAction.setToolTipText(Messages.getString("action.link.desc")); //$NON-NLS-1$
    linkToEditorAction.setImageDescriptor(SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONARSYNCHRO));

    // Create toolbar
    IActionBars bars = getViewSite().getActionBars();
    bars.getToolBarManager().add(linkToEditorAction);

    // TODO comment me
    getSite().getPage().addPartListener(partListener2);
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void dispose() {
    getSite().getPage().removePartListener(partListener2);
  }

  protected void toggleLinking(boolean checked) {
    this.linking = checked;
    if (this.linking) {
      editorActivated(getSite().getPage().getActiveEditor());
    }
  }

  protected void editorActivated(IEditorPart editor) {
    if (editor == null) {
      return;
    }
    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput == null) {
      return;
    }
    final IResource resource = getResourceFromEditor(editorInput);
    if (resource == null) {
      return;
    }
    // TODO Godin: should be refactored
    new Job("My new job") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Some nice progress message here ...", 100);

        // execute the task ...
        String resourceKey = EclipseResourceUtils.getInstance().getFileKey(resource);
        IProject project = resource.getProject();
        ProjectProperties properties = ProjectProperties.getInstance(project);
        Sonar sonar = SonarPlugin.getServerManager().getSonar(properties.getUrl());
        final List<MeasureData> measures = new EclipseSonar(sonar).search(resourceKey).getMeasures();
        Display.getDefault().asyncExec(new Runnable() {

          public void run() {
            viewer.setInput(measures);
          }
        });

        monitor.done();
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  private IResource getResourceFromEditor(IEditorInput editorInput) {
    IJavaElement element = JavaUI.getEditorInputJavaElement(editorInput);
    if (element != null) {
      return element.getResource();
    }
    return null;
  }

  private final IPartListener2 partListener2 = new IPartListener2() {

    public void partActivated(IWorkbenchPartReference ref) {
      if (ref.getPart(true) instanceof IEditorPart) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partBroughtToTop(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partClosed(IWorkbenchPartReference ref) {
    }

    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    public void partHidden(IWorkbenchPartReference ref) {
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
    }

    public void partOpened(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partVisible(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }
  };

}
