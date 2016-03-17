/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import org.eclipse.jface.internal.text.html.HTML2TextReader;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.sonarlint.eclipse.core.internal.resources.IAssociatedProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;

public class ServerToolTip extends ToolTip {
  protected static Shell CURRENT_TOOLTIP;
  protected Label hintLabel;
  protected Tree tree;
  protected int x;
  protected int y;

  public ServerToolTip(final Tree tree) {
    super(tree);

    this.tree = tree;

    tree.addMouseMoveListener(new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        x = e.x;
        y = e.y;
      }
    });

    tree.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e == null)
          return;

        if (e.keyCode == SWT.ESC) {
          if (CURRENT_TOOLTIP != null) {
            CURRENT_TOOLTIP.dispose();
            CURRENT_TOOLTIP = null;
          }
          activate();
        }
        if (e.keyCode == SWT.F6 && CURRENT_TOOLTIP == null) {
          deactivate();
          hide();
          createFocusedTooltip(tree);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        // nothing to do
      }
    });

  }

  public void createFocusedTooltip(final Control control) {
    final Shell stickyTooltip = new Shell(control.getShell(), SWT.ON_TOP | SWT.TOOL
      | SWT.NO_FOCUS);
    stickyTooltip.setLayout(new FillLayout());
    stickyTooltip.setBackground(stickyTooltip.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    stickyTooltip.addShellListener(new ShellAdapter() {
      @Override
      public void shellClosed(ShellEvent e) {
        if (CURRENT_TOOLTIP != null) {
          CURRENT_TOOLTIP.dispose();
          CURRENT_TOOLTIP = null;
        }
        activate();
      }
    });

    control.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        Event event = new Event();
        event.x = x;
        event.y = y;
        event.widget = tree;

        createToolTipContentArea(event, stickyTooltip);
        stickyTooltip.pack();

        stickyTooltip.setLocation(stickyTooltip.getDisplay().getCursorLocation());
        hintLabel.setText(Messages.toolTipDisableFocus);
        stickyTooltip.setVisible(true);
      }
    });
    CURRENT_TOOLTIP = stickyTooltip;
  }

  @Override
  protected Object getToolTipArea(Event event) {
    return tree.getItem(new Point(event.x, event.y));
  }

  @Override
  protected final boolean shouldCreateToolTip(Event event) {
    Object o = tree.getItem(new Point(event.x, event.y));
    if (o == null) {
      return false;
    }
    IServer server = null;
    IAssociatedProject project = null;
    if (o instanceof TreeItem) {
      Object obj = ((TreeItem) o).getData();
      if (obj instanceof IServer) {
        server = (IServer) obj;
      }
      if (obj instanceof IAssociatedProject) {
        project = (IAssociatedProject) obj;
      }
    }
    // Only enable for supported objects.
    if (server == null && project == null) {
      return false;
    }
    return super.shouldCreateToolTip(event);
  }

  @Override
  protected Composite createToolTipContentArea(Event event, Composite parent) {
    Object o = tree.getItem(new Point(event.x, event.y));
    if (o == null)
      return null;

    IServer server = null;
    IAssociatedProject project = null;
    if (o instanceof TreeItem) {
      Object obj = ((TreeItem) o).getData();
      if (obj instanceof IServer)
        server = (IServer) obj;
      if (obj instanceof IAssociatedProject)
        project = (IAssociatedProject) obj;
    }

    FillLayout layout = (FillLayout) parent.getLayout();
    layout.type = SWT.VERTICAL;
    parent.setLayout(layout);
    parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

    // set the default text for the tooltip
    StyledText sText = new StyledText(parent, SWT.NONE);
    sText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
    sText.setEditable(false);
    sText.setBackground(parent.getBackground());

    if (project != null) {
      sText.setText("<b>" + project.getName() + "</b>");

      StyledText sText2 = new StyledText(parent, SWT.NONE);
      sText2.setEditable(false);
      sText2.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
      sText2.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

      sText2.setText(project.getRemoteKey());
    }

    if (server != null) {
      sText.setText("<b>" + server.getName() + "</b>");
    }

    // add the F3 text
    hintLabel = new Label(parent, SWT.BORDER);
    hintLabel.setAlignment(SWT.RIGHT);
    hintLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    hintLabel.setText(Messages.toolTipEnableFocus);
    hintLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

    final Font font;
    Display display = parent.getDisplay();
    FontData[] fd = parent.getFont().getFontData();
    int size2 = fd.length;
    for (int i = 0; i < size2; i++)
      fd[i].setHeight(7);
    font = new Font(display, fd);
    parent.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        font.dispose();
      }
    });
    hintLabel.setFont(font);

    parseText(sText.getText(), sText);

    return parent;
  }

  protected void parseText(String htmlText, StyledText sText) {
    TextPresentation presentation = new TextPresentation();
    HTML2TextReader reader = new HTML2TextReader(new StringReader(htmlText), presentation);
    String text;

    try {
      text = reader.getString();
    } catch (IOException e) {
      text = ""; //$NON-NLS-1$
    }

    sText.setText(text);
    Iterator iter = presentation.getAllStyleRangeIterator();
    while (iter.hasNext()) {
      StyleRange sr = (StyleRange) iter.next();
      sText.setStyleRange(sr);
    }
  }

}
