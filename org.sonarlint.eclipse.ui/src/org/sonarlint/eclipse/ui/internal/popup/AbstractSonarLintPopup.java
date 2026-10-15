/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.ui.internal.popup;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/** Base class for SonarLint notification popups, stacked above each other in the bottom right corner of the workbench window. */
public abstract class AbstractSonarLintPopup extends AbstractNotificationPopup {

  private static final int MAX_WIDTH = 400;
  private static final int MIN_HEIGHT = 100;
  private static final int PADDING_EDGE = 5;
  private static final int PADDING_TOP = 30;

  private static final List<AbstractSonarLintPopup> popupStack = new LinkedList<>();

  private static final Listener relayoutListener = e -> inSameParentShell((Shell) e.widget).forEach(AbstractSonarLintPopup::initializeBounds);

  // Eclipse 4.17 gives the popup a hidden parent shell when none is set, so getParentShell() can't tell
  @Nullable
  private Shell workbenchShell;

  // The hidden parent shell of Eclipse 4.17, which nothing else disposes
  @Nullable
  private Shell placeholderShell;

  // The platform adds the same listener itself only since Eclipse 4.24
  private final MouseListener parentShellActivator = new MouseAdapter() {
    @Override
    public void mouseDown(MouseEvent e) {
      var parentShell = workbenchShell;
      if (parentShell != null) {
        parentShell.setActive();
        parentShell.moveAbove(null);
      }
    }
  };

  protected AbstractSonarLintPopup() {
    // No SWT.NO_FOCUS, it keeps a click on the popup from activating the workbench window (bug 579019)
    super(Display.getDefault(), SWT.NO_TRIM | SWT.ON_TOP | SWT.TOOL);
    setDelayClose(0);
    placeholderShell = getParentShell();
    var parentShell = findParentShell();
    if (parentShell != null) {
      setParentShell(parentShell);
      disposePlaceholderShell();
    }
  }

  private void disposePlaceholderShell() {
    if (placeholderShell != null && !placeholderShell.isDisposed()) {
      placeholderShell.dispose();
    }
    placeholderShell = null;
  }

  private static List<AbstractSonarLintPopup> inSameParentShell(@Nullable Shell parent) {
    return popupStack.stream().filter(p -> p.workbenchShell == parent).collect(Collectors.toList());
  }

  @Override
  protected void setParentShell(Shell newParentShell) {
    super.setParentShell(newParentShell);
    workbenchShell = newParentShell;
    if (!List.of(newParentShell.getListeners(SWT.Resize)).contains(relayoutListener)) {
      newParentShell.addListener(SWT.Resize, relayoutListener);
      newParentShell.addListener(SWT.Move, relayoutListener);
    }
  }

  @Override
  public int open() {
    popupStack.add(this);
    return super.open();
  }

  @Override
  public boolean close() {
    popupStack.remove(this);
    inSameParentShell(workbenchShell).forEach(AbstractSonarLintPopup::initializeBounds);
    var closed = super.close();
    disposePlaceholderShell();
    return closed;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setBackgroundMode(SWT.INHERIT_FORCE);
    newShell.setText(getPopupShellTitle());
  }

  @Override
  protected void createTitleArea(Composite parent) {
    super.createTitleArea(parent);
    var children = parent.getChildren();
    // The last child is the close button
    for (var i = 0; i < children.length - 1; i++) {
      children[i].addMouseListener(parentShellActivator);
    }
  }

  @Override
  protected void initializeBounds() {
    var shell = getShell();
    var clientArea = getPrimaryClientArea(shell);
    var initialSize = shell.computeSize(MAX_WIDTH, SWT.DEFAULT);
    var size = new Point(Math.min(initialSize.x, MAX_WIDTH), Math.max(initialSize.y, MIN_HEIGHT));

    var startY = clientArea.y + clientArea.height - PADDING_EDGE;
    var siblings = inSameParentShell(workbenchShell);
    var index = siblings.indexOf(this);
    if (index > 0) {
      startY = siblings.get(index - 1).getShell().getLocation().y;
    }

    shell.setLocation(clientArea.x + clientArea.width - size.x - PADDING_EDGE, Math.max(clientArea.y + PADDING_TOP, startY - size.y));
    shell.setSize(size);
  }

  private Rectangle getPrimaryClientArea(Shell shell) {
    var parentShell = workbenchShell;
    if (parentShell != null) {
      var bounds = parentShell.getBounds();
      var trim = parentShell.computeTrim(0, 0, 0, 0);
      return new Rectangle(bounds.x - trim.x, bounds.y - trim.y, bounds.width - trim.width, bounds.height - trim.height);
    }
    var primaryMonitor = shell.getDisplay().getPrimaryMonitor();
    return primaryMonitor != null ? primaryMonitor.getClientArea() : shell.getDisplay().getClientArea();
  }

  @Nullable
  private static Shell findParentShell() {
    var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      var windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      if (windows.length > 0) {
        return windows[0].getShell();
      }
    } else {
      return window.getShell();
    }
    return null;
  }

  private Composite linksContainer;

  @Override
  protected void createContentArea(Composite parent) {
    parent.addMouseListener(parentShellActivator);
    var messageLabel = new Label(parent, SWT.WRAP);
    var messageLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    messageLabel.setLayoutData(messageLayoutData);

    messageLabel.setText(getMessage());

    linksContainer = new Composite(parent, SWT.NONE);
    var linksLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    linksLayoutData.horizontalAlignment = SWT.END;
    linksLayoutData.verticalAlignment = SWT.BOTTOM;
    linksContainer.setLayoutData(linksLayoutData);

    var rowLayout = new RowLayout();
    rowLayout.spacing = 20;
    linksContainer.setLayout(rowLayout);
  }

  protected abstract String getMessage();

  protected void addLink(String text, Consumer<SelectionEvent> selectionHandler) {
    addLinkWithTooltip(text, null, selectionHandler);
  }

  protected void addLinkWithTooltip(String text, @Nullable String tooltipText, Consumer<SelectionEvent> selectionHandler) {
    var detailsLink = new Link(linksContainer, SWT.NONE);
    detailsLink.setText("<a>" + text + "</a>");
    if (tooltipText != null) {
      detailsLink.setToolTipText(tooltipText);
    }
    detailsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectionHandler.accept(e);
      }
    });
  }
}
