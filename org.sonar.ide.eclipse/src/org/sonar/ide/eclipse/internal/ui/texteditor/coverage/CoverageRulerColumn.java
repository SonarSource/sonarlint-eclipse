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

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AnnotationPreference;

/**
 * @author Jérémie Lagarde
 * @since 0.2.0
 */
public class CoverageRulerColumn implements IVerticalRulerColumn {

  // This column's parent ruler
  private CompositeRuler   parentRuler;
  // The ruler's annotation model.
  private IAnnotationModel annotationModel;
  // Cached text viewer
  private ITextViewer      cachedTextViewer;
  // Cached text widget
  private StyledText       cachedTextWidget;
  // The ruler's canvas
  private Canvas           control;
  // The font of this column
  private Font             font;
  // Cache for the actual scroll position in pixels
  private int              scrollPos;
  // The drawable for double buffering
  private Image            buffer;
  // The internal listener
  private InternalListener internalListener = new InternalListener();
  private int[]            indentationTab;

  public Control createControl(CompositeRuler parentRuler, Composite parentControl) {

    this.parentRuler = parentRuler;
    cachedTextViewer = parentRuler.getTextViewer();
    cachedTextWidget = cachedTextViewer.getTextWidget();

    control = new Canvas(parentControl, SWT.NO_FOCUS);
    // set background
    Color background = cachedTextViewer.getTextWidget().getBackground();
    control.setBackground(background);
    cachedTextViewer = parentRuler.getTextViewer();
    cachedTextWidget = cachedTextViewer.getTextWidget();

    control.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent event) {
        if (cachedTextViewer != null) {
          doubleBufferPaint(event.gc);
        }
      }
    });

    control.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        handleDispose();
        cachedTextViewer = null;
        cachedTextWidget = null;
      }
    });

    if (cachedTextViewer != null) {
      cachedTextViewer.addViewportListener(internalListener);
      cachedTextViewer.addTextListener(internalListener);
      if (font == null) {
        if (cachedTextWidget != null && !cachedTextWidget.isDisposed()) {
          font = cachedTextWidget.getFont();
        }
      }
    }

    if (font != null) {
      control.setFont(font);
    }
    computeIndentations();

    return control;
  }

  /**
   * Disposes the column's resources.
   */
  protected void handleDispose() {

    if (cachedTextViewer != null) {
      cachedTextViewer.removeViewportListener(internalListener);
      cachedTextViewer.removeTextListener(internalListener);
    }

    if (buffer != null) {
      buffer.dispose();
      buffer = null;
    }
  }

  public Control getControl() {
    return control;
  }

  public int getWidth() {
    return indentationTab[0];
  }

  /**
   * Triggers a redraw in the display thread.
   */
  protected final void postRedraw() {
    if (control != null && !control.isDisposed()) {
      Display d = control.getDisplay();
      if (d != null) {
        d.asyncExec(new Runnable() {
          public void run() {
            redraw();
          }
        });
      }
    }
  }

  /*
   * @see IVerticalRulerColumn#redraw()
   */
  public void redraw() {
    if (control != null && !control.isDisposed()) {
      GC gc = new GC(control);
      doubleBufferPaint(gc);
      gc.dispose();
    }
  }

  public void setFont(Font font) {
    this.font = font;
    if (getControl() != null && !getControl().isDisposed()) {
      getControl().setFont(font);
      computeIndentations();
    }
  }

  public void setModel(IAnnotationModel model) {
    annotationModel = model;
    postRedraw();
  }

  public IAnnotationModel getModel() {
    return annotationModel;
  }

  /**
   * Double buffer drawing.
   * 
   * @param dest the GC to draw into
   */
  private void doubleBufferPaint(GC dest) {

    Point size = getControl().getSize();

    if (size.x <= 0 || size.y <= 0) {
      return;
    }

    if (buffer != null) {
      Rectangle r = buffer.getBounds();
      if (r.width != size.x || r.height != size.y) {
        buffer.dispose();
        buffer = null;
      }
    }
    if (buffer == null) {
      buffer = new Image(getControl().getDisplay(), size.x, size.y);
    }

    GC gc = new GC(buffer);
    gc.setFont(getControl().getFont());
    if (getControl().getForeground() != null) {
      gc.setForeground(getControl().getForeground());
    }

    try {
      gc.setBackground(getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
      gc.fillRectangle(0, 0, size.x, size.y);

      ILineRange visibleLines = JFaceTextUtil.getVisibleModelLines(cachedTextViewer);
      if (visibleLines == null) {
        return;
      }
      doPaint(gc, visibleLines);
    } finally {
      gc.dispose();
    }

    scrollPos = cachedTextWidget.getTopPixel();
    dest.drawImage(buffer, 0, 0);
  }

  void doPaint(GC gc, ILineRange visibleLines) {
    Display display = cachedTextWidget.getDisplay();

    int y = -JFaceTextUtil.getHiddenTopLinePixels(cachedTextWidget);

    int lastLine = visibleLines.getStartLine() + visibleLines.getNumberOfLines();
    for (int line = visibleLines.getStartLine(); line < lastLine; line++) {
      int widgetLine = JFaceTextUtil.modelLineToWidgetLine(cachedTextViewer, line);
      if (widgetLine == -1) {
        continue;
      }

      int lineHeight = cachedTextWidget.getLineHeight(cachedTextWidget.getOffsetAtLine(widgetLine));
      paintLine(line, y, lineHeight, gc, display);
      y += lineHeight;
    }
  }

  @SuppressWarnings("restriction")
  protected void paintLine(int line, int y, int lineheight, GC gc, Display display) {
    Annotation annotation = findAnnotation(line);
    if (annotation == null) {
      return;
    }
    int widgetLine = JFaceTextUtil.modelLineToWidgetLine(cachedTextViewer, line);

    String s = annotation.getText();
    int indentation = indentationTab[s.length()];
    int baselineBias = getBaselineBias(gc, widgetLine);

    // TODO : find a better way to do that.
    AnnotationPreference preference = EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
    if (preference != null) {
      gc.setBackground(new Color(display, preference.getColorPreferenceValue()));
      gc.fillRectangle(0, y, indentationTab[0], lineheight);
      gc.setForeground(new Color(display, 119, 119, 119));
      gc.drawString(s, indentation, y + baselineBias, true);
    }
  }

  private int getBaselineBias(GC gc, int widgetLine) {
    int offset = cachedTextWidget.getOffsetAtLine(widgetLine);
    int widgetBaseline = cachedTextWidget.getBaseline(offset);

    FontMetrics fm = gc.getFontMetrics();
    int fontBaseline = fm.getAscent() + fm.getLeading();
    int baselineBias = widgetBaseline - fontBaseline;
    return Math.max(0, baselineBias);
  }

  protected void computeIndentations() {
    if (getControl() == null || getControl().isDisposed()) {
      return;
    }

    GC gc = new GC(getControl());
    try {

      gc.setFont(getControl().getFont());
      indentationTab = new int[7 + 1];
      char[] nines = new char[7];
      Arrays.fill(nines, '9');
      String nineString = new String(nines);
      Point p = gc.stringExtent(nineString);
      indentationTab[0] = p.x;
      for (int i = 1; i <= 7; i++) {
        p = gc.stringExtent(nineString.substring(0, i));
        indentationTab[i] = indentationTab[0] - p.x;
      }
    } finally {
      gc.dispose();
    }
  }

  /**
   * Returns the coverage annotation of the column's annotation model that
   * contains the given line.
   * 
   * @param line the line
   * @return the coverage annotation containing the given line
   */
  private Annotation findAnnotation(int lineNumber) {

    IAnnotationModel model = getModel();
    if (model != null) {
      IRegion line;
      try {
        IDocument d = cachedTextViewer.getDocument();
        if (d == null) {
          return null;
        }

        line = d.getLineInformation(lineNumber);
      } catch (BadLocationException ex) {
        return null;
      }

      int lineStart = line.getOffset();
      int lineLength = line.getLength();

      Iterator e = model.getAnnotationIterator();
      while (e.hasNext()) {
        Object next = e.next();
        if (next instanceof Annotation) {
          Annotation annotation = (Annotation) next;
          Annotation a = null;
          if (annotation.getType().equals("org.sonar.ide.eclipse.fullCoverageAnnotationType")) {
            a = annotation;
          }
          if (annotation.getType().equals("org.sonar.ide.eclipse.partialCoverageAnnotationType")) {
            a = annotation;
          }
          if (annotation.getType().equals("org.sonar.ide.eclipse.noCoverageAnnotationType")) {
            a = annotation;
          }

          if (a == null) {
            continue;
          }

          if (a.isMarkedDeleted()) {
            continue;
          }

          Position p = model.getPosition(a);
          if (p == null || p.isDeleted()) {
            continue;
          }

          if (p.overlapsWith(lineStart, lineLength) || p.length == 0 && p.offset == lineStart + lineLength) {
            return a;
          }
        }
      }
    }

    return null;
  }

  /**
   * Internal listener class.
   */
  class InternalListener implements IViewportListener, ITextListener {
    public void viewportChanged(int verticalPosition) {
      if (verticalPosition != scrollPos) {
        redraw();
      }
    }

    public void modelChanged(IAnnotationModel model) {
      postRedraw();
    }

    public void textChanged(TextEvent e) {
      if (e.getViewerRedrawState()) {
        postRedraw();
      }
    }
  }

}
