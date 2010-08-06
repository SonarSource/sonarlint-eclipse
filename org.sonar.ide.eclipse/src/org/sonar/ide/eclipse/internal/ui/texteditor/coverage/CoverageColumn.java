/*
 * Copyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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
package org.sonar.ide.eclipse.internal.ui.texteditor.coverage;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.rulers.AbstractContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.IColumnSupport;
import org.sonar.ide.eclipse.jobs.RefreshCoverageJob;

/**
 * @author Jérémie Lagarde
 * @since 0.2.0
 */
public class CoverageColumn extends AbstractContributedRulerColumn {

  /**
   * Preference key for showing the coverage ruler.
   */
  final static String             SONAR_COVERAGE_RULER = "sonarCoverageRuler";
  final static String             ID                   = "org.sonar.ide.eclipse.coverageRuler";

  private IVerticalRulerColumn    delegate;
  private IPropertyChangeListener propertyChangeListener;

  public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
    initialize();
    Control control = delegate.createControl(parentRuler, parentControl);
    showCoverage(isCoverageRulerVisible());

    propertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (SONAR_COVERAGE_RULER.equals(event.getProperty())) {
          showCoverage(isCoverageRulerVisible());
        }
      }
    };
    final IPreferenceStore store = EditorsUI.getPreferenceStore();
    store.addPropertyChangeListener(propertyChangeListener);
    // TODO  need to to it before release ...
    //    control.addDisposeListener(new DisposeListener() {
    //      public void widgetDisposed(DisposeEvent e) {
    //        store.removePropertyChangeListener(propertyChangeListener);
    //      }
    //    });
    return control;
  }

  public Control getControl() {
    return delegate.getControl();
  }

  public int getWidth() {
    return delegate.getWidth();
  }

  public void redraw() {
    delegate.redraw();
  }

  public void setFont(Font font) {
    delegate.setFont(font);
  }

  public void setModel(IAnnotationModel model) {
    initialize();
    delegate.setModel(model);
  }

  private void initialize() {
    if (delegate == null) {
      delegate = new CoverageRulerColumn();
    }
  }

  /**
   * Returns whether the sonar coverage ruler column should be visible according
   * to the preference store settings.
   */
  private boolean isCoverageRulerVisible() {
    IPreferenceStore store = EditorsUI.getPreferenceStore();
    return store != null ? store.getBoolean(CoverageColumn.SONAR_COVERAGE_RULER) : false;
  }

  private void showCoverage(boolean show) {
    IColumnSupport columnSupport = (IColumnSupport) getEditor().getAdapter(IColumnSupport.class);
    if (show && delegate != null) {
      columnSupport.setColumnVisible(getDescriptor(), true);
      final Job job = new RefreshCoverageJob((AbstractDecoratedTextEditor) getEditor());
      job.schedule();
    } else {
      columnSupport.setColumnVisible(getDescriptor(), false);
      getEditor().getAdapter(IResource.class);
      try {
        final IAnnotationModel model = getEditor().getDocumentProvider().getAnnotationModel(getEditor().getEditorInput());
        for (Iterator iterator = model.getAnnotationIterator(); iterator.hasNext();) {
          Annotation annotation = (Annotation) iterator.next();
          if (annotation.getType().equals("org.sonar.ide.eclipse.fullCoverageAnnotationType")
              || annotation.getType().equals("org.sonar.ide.eclipse.partialCoverageAnnotationType")
              || annotation.getType().equals("org.sonar.ide.eclipse.noCoverageAnnotationType")) {
            model.removeAnnotation(annotation);
          }
        }
      } catch (Exception e) {
      }
    }
  }
}
