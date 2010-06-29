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

import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.rulers.AbstractContributedRulerColumn;

/**
 * @author Jérémie Lagarde
 * @since 0.2.0
 */
public class CoverageColumn extends AbstractContributedRulerColumn {

  private IVerticalRulerColumn delegate;

  public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
    initialize();
    Control control = delegate.createControl(parentRuler, parentControl);
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
}
