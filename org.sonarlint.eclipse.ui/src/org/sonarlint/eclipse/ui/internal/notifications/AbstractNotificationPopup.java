/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.notifications;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.notifications.internal.AnimationUtil;
import org.sonarlint.eclipse.ui.internal.notifications.internal.AnimationUtil.FadeJob;
import org.sonarlint.eclipse.ui.internal.notifications.internal.Messages;

/**
 * Inspired from org.eclipse.jface.notifications
 * https://git.eclipse.org/c/platform/eclipse.platform.ui.git/tree/bundles/org.eclipse.jface.notifications/src/org/eclipse/jface/notifications/AbstractNotificationPopup.java
 *
 */
public abstract class AbstractNotificationPopup extends Window {

  static final int TITLE_HEIGHT = 24;

  private static final String LABEL_NOTIFICATION = Messages.AbstractNotificationPopup_Label;

  private static final String LABEL_JOB_CLOSE = Messages.AbstractNotificationPopup_CloseJobTitle;

  private static final int MAX_WIDTH = 400;

  private static final int MIN_HEIGHT = 100;

  private static final long DEFAULT_DELAY_CLOSE = Duration.ofSeconds(8).toMillis();

  private static final int PADDING_EDGE = 5;
  private static final int PADDING_TOP = 30;

  private static final String LABEL_SDK = "SDK"; //$NON-NLS-1$

  private long delayClose = DEFAULT_DELAY_CLOSE;

  protected LocalResourceManager resources;

  private final Display display;

  @Nullable
  private Shell shell;

  private static final LinkedList<AbstractNotificationPopup> popupStack = new LinkedList<>();

  private final Job closeJob = new Job(LABEL_JOB_CLOSE) {

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      if (!AbstractNotificationPopup.this.display.isDisposed()) {
        AbstractNotificationPopup.this.display.asyncExec(() -> {
          var shell = AbstractNotificationPopup.this.getShell();
          if (shell == null || shell.isDisposed()) {
            return;
          }

          if (isMouseOver(shell)) {
            scheduleAutoClose();
            return;
          }

          AbstractNotificationPopup.this.closeFade();
        });
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      return Status.OK_STATUS;
    }
  };

  @Nullable
  private FadeJob fadeJob;

  private boolean fadingEnabled;

  private static Listener resizeListener = new Listener() {
    @Override
    public void handleEvent(Event e) {
      var resized = (Shell) e.widget;
      inSameParentShell(resized).forEach(AbstractNotificationPopup::initializeBounds);
    }
  };

  private static List<AbstractNotificationPopup> inSameParentShell(Shell parent) {
    return popupStack.stream().filter(p -> p.getParentShell().equals(parent)).collect(Collectors.toList());
  }

  @Nullable
  private MouseListener windowActivationHelper;

  protected AbstractNotificationPopup(Display display) {
    // Bug 579019: NO_FOCUS leads to problems with main Window not being activated
    // anymore if it is not visible but the notification shell is.
    this(display, SWT.NO_TRIM | SWT.ON_TOP | SWT.TOOL);
  }

  protected AbstractNotificationPopup(Display display, int style) {
    super((Shell) null);
    setShellStyle(style);

    this.display = display;
    this.resources = new LocalResourceManager(JFaceResources.getResources());
    this.closeJob.setSystem(true);
  }

  /**
   * Overrides default implementation to add window activation helper.
   * <p>
   * {@inheritDoc}
   */
  @Override
  protected void setParentShell(Shell newParentShell) {
    super.setParentShell(newParentShell);
    windowActivationHelper = createWindowActivationHelper(newParentShell);

    if (!List.of(newParentShell.getListeners(SWT.Resize)).contains(resizeListener)) {
      newParentShell.addListener(SWT.Resize, resizeListener);
      newParentShell.addListener(SWT.Move, resizeListener);
    }
  }

  /**
   * Creates listener that shows and activates the main Eclipse window by clicking
   * on the popup control if it was not in foreground.
   * <p>
   * Clients can override if the window activation shouldn't be added to the popup
   * or a different behavior is desired by clicking on the popup.
   *
   * @param parentShell parent shell for this popup
   * @return {@code null} if no window activation should be added to the popup
   * @since 0.5
   */
  protected MouseListener createWindowActivationHelper(final Shell parentShell) {
    return new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        // if not a top-level shell: show parent window notification on notification
        // click
        if (parentShell != null) {
          parentShell.setActive();
          parentShell.moveAbove(null);
        }
      }
    };
  }

  public boolean isFadingEnabled() {
    return this.fadingEnabled;
  }

  public void setFadingEnabled(boolean fadingEnabled) {
    this.fadingEnabled = fadingEnabled;
  }

  @Override
  public int open() {
    popupStack.add(this);
    if (this.shell == null || this.shell.isDisposed()) {
      this.shell = null;
      create();
    }

    constrainShellSize();

    if (isFadingEnabled()) {
      this.shell.setAlpha(0);
    }
    this.shell.setVisible(true);
    this.fadeJob = AnimationUtil.fadeIn(this.shell, (shell, alpha) -> {
      if (shell.isDisposed()) {
        return;
      }

      if (alpha == 255) {
        scheduleAutoClose();
      }
    });

    return Window.OK;
  }

  @Override
  public boolean close() {
    this.resources.dispose();
    popupStack.remove(this);
    inSameParentShell(this.getParentShell()).forEach(AbstractNotificationPopup::initializeBounds);
    return super.close();
  }

  public long getDelayClose() {
    return this.delayClose;
  }

  public void setDelayClose(long delayClose) {
    this.delayClose = delayClose;
  }

  public void closeFade() {
    if (this.fadeJob != null) {
      this.fadeJob.cancelAndWait(false);
    }
    this.fadeJob = AnimationUtil.fadeOut(getShell(), (shell, alpha) -> {
      if (!shell.isDisposed()) {
        if (alpha == 0) {
          shell.close();
        } else if (isMouseOver(shell)) {
          if (AbstractNotificationPopup.this.fadeJob != null) {
            AbstractNotificationPopup.this.fadeJob.cancelAndWait(false);
          }
          AbstractNotificationPopup.this.fadeJob = AnimationUtil.fastFadeIn(shell, (shell1, alpha1) -> {
            if (shell1.isDisposed()) {
              return;
            }

            if (alpha1 == 255) {
              scheduleAutoClose();
            }
          });
        }
      }
    });
  }

  /**
   * Override to return a customized name. Default is to return the name of the product, specified by the -name (e.g.
   * "Eclipse SDK") command line parameter that's associated with the product ID (e.g. "org.eclipse.sdk.ide"). Strips
   * the trailing "SDK" for any name, since this part of the label is considered visual noise.
   *
   * @return the name to be used in the title of the popup.
   */
  protected String getPopupShellTitle() {
    String productName = getProductName();
    if (productName != null) {
      return productName + " " + LABEL_NOTIFICATION; //$NON-NLS-1$
    } else {
      return LABEL_NOTIFICATION;
    }
  }

  @Nullable
  protected Image getPopupShellImage(int maximumHeight) {
    return null;
  }

  /**
   * Override to populate with notifications.
   *
   * @param parent Parent for this component.
   */
  protected void createContentArea(Composite parent) {
    // empty by default
  }

  /**
   * Override to customize the title bar
   */
  protected void createTitleArea(Composite parent) {
    ((GridData) parent.getLayoutData()).heightHint = TITLE_HEIGHT;

    var titleImageLabel = new Label(parent, SWT.NONE);
    titleImageLabel.setImage(getPopupShellImage(TITLE_HEIGHT));

    var titleTextLabel = new Label(parent, SWT.NONE);
    titleTextLabel.setText(getPopupShellTitle());
    titleTextLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
    titleTextLabel.setForeground(getTitleForeground());
    titleTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    titleTextLabel.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

    createCloseButton(parent);
    addWindowActivationHelper(titleTextLabel);
  }

  void createCloseButton(Composite parent) {
    final var button = new Label(parent, SWT.NONE);
    button.setImage(SonarLintImages.NOTIFICATION_CLOSE);
    button.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        button.setImage(SonarLintImages.NOTIFICATION_CLOSE_HOVER);
      }

      @Override
      public void mouseExit(MouseEvent e) {
        button.setImage(SonarLintImages.NOTIFICATION_CLOSE);
      }
    });
    button.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseUp(MouseEvent e) {
        close();
        setReturnCode(CANCEL);
      }

    });
  }

  protected Color getTitleForeground() {
    return display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    this.shell = newShell;
    this.shell.setBackgroundMode(SWT.INHERIT_FORCE);
    this.shell.setText(getPopupShellTitle());
  }

  protected void scheduleAutoClose() {
    if (this.delayClose > 0) {
      this.closeJob.schedule(this.delayClose);
    }
  }

  /**
   * Allows to add activation listener to custom control. The listener shows and
   * activates the main Eclipse window by clicking on the control if the window
   * was not in foreground.
   * <p>
   * Clients can override to disable window activation on popup clicking
   *
   * @since 0.5
   */
  protected void addWindowActivationHelper(Control control) {
    if (windowActivationHelper != null) {
      control.addMouseListener(windowActivationHelper);
    }
  }

  @Override
  protected Control createContents(Composite parent) {
    ((GridLayout) parent.getLayout()).marginWidth = 1;
    ((GridLayout) parent.getLayout()).marginHeight = 1;

    /* Outer Composite holding the controls */
    final var outerCircle = new Composite(parent, SWT.NO_FOCUS);
    outerCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    var layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;

    outerCircle.setLayout(layout);

    /* Title area containing label and close button */
    final var titleCircle = new Composite(outerCircle, SWT.NO_FOCUS);
    titleCircle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    layout = new GridLayout(4, false);
    layout.marginWidth = 3;
    layout.marginHeight = 0;
    layout.verticalSpacing = 5;
    layout.horizontalSpacing = 3;

    titleCircle.setLayout(layout);

    /* Create Title Area */
    createTitleArea(titleCircle);

    /* Outer composite to hold content controls */
    var outerContentCircle = new Composite(outerCircle, SWT.NONE);

    layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;

    outerContentCircle.setLayout(layout);
    outerContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Middle composite to show a 1px black line around the content controls */
    var middleContentCircle = new Composite(outerContentCircle, SWT.NO_FOCUS);

    layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.marginTop = 1;

    middleContentCircle.setLayout(layout);
    middleContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Inner composite containing the content controls */
    var innerContent = new Composite(middleContentCircle, SWT.NO_FOCUS);
    innerContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 5;
    layout.marginLeft = 5;
    layout.marginRight = 5;
    innerContent.setLayout(layout);

    innerContent.setBackground(this.shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* Content Area */
    createContentArea(innerContent);
    addWindowActivationHelper(innerContent);
    return outerCircle;
  }

  @Override
  protected void initializeBounds() {
    var clArea = getPrimaryClientArea();
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=539794
    var initialSize = this.shell.computeSize(MAX_WIDTH, SWT.DEFAULT);
    var height = Math.max(initialSize.y, MIN_HEIGHT);
    var width = Math.min(initialSize.x, MAX_WIDTH);

    var startY = clArea.height + clArea.y - PADDING_EDGE;

    var inSameParentShell = inSameParentShell(this.getParentShell());
    var myIndex = inSameParentShell.indexOf(this);
    if (myIndex > 0) {
      var popupBelow = inSameParentShell.get(myIndex - 1).getShell();
      startY = popupBelow.getLocation().y;
    }

    var size = new Point(width, height);
    this.shell.setLocation(clArea.width + clArea.x - size.x - PADDING_EDGE, Math.max(clArea.y + PADDING_TOP, startY - size.y));
    this.shell.setSize(size);
  }

  @Nullable
  private static String getProductName() {
    var product = Platform.getProduct();
    if (product != null) {
      var productName = product.getName();
      if (productName != null) {
        if (productName.endsWith(LABEL_SDK)) {
          productName = productName.substring(0, productName.length() - LABEL_SDK.length()).trim();
        }
        return productName;
      }
    }
    return null;
  }

  private boolean isMouseOver(Shell shell) {
    if (this.display.isDisposed()) {
      return false;
    }
    return shell.getBounds().contains(this.display.getCursorLocation());
  }

  private Rectangle getPrimaryClientArea() {
    var parentShell = getParentShell();
    if (parentShell != null) {
      // calculate client area in display-relative coordinates
      // (i.e. without window border / decorations)
      var bounds = parentShell.getBounds();
      var trim = parentShell.computeTrim(0, 0, 0, 0);
      return new Rectangle(bounds.x - trim.x, bounds.y - trim.y, bounds.width - trim.width,
        bounds.height - trim.height);
    }
    // else display on primary monitor
    var primaryMonitor = this.shell.getDisplay().getPrimaryMonitor();
    return (primaryMonitor != null) ? primaryMonitor.getClientArea() : this.shell.getDisplay().getClientArea();
  }

}
