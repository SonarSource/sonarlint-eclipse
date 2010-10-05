package org.sonar.ide.eclipse.ui;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ObservableValueEditingSupport;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * see Snippet013TableViewerEditing
 */
public class InlineEditingSupport extends ObservableValueEditingSupport {
  private CellEditor cellEditor;
  private String propName;

  public InlineEditingSupport(ColumnViewer viewer, DataBindingContext dbc, String propName) {
    super(viewer, dbc);
    cellEditor = new TextCellEditor((Composite) viewer.getControl());
    this.propName = propName;
  }

  protected CellEditor getCellEditor(Object element) {
    return cellEditor;
  }

  protected IObservableValue doCreateCellEditorObservable(CellEditor cellEditor) {
    return SWTObservables.observeText(cellEditor.getControl(), SWT.Modify);
  }

  protected IObservableValue doCreateElementObservable(Object element, ViewerCell cell) {
    return BeansObservables.observeValue(element, propName);
  }
}
