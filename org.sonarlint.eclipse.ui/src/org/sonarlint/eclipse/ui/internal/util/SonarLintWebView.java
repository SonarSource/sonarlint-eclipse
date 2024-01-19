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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

public class SonarLintWebView extends Composite implements Listener, IPropertyChangeListener {

  private static final RGB DEFAULT_ACTIVE_LINK_COLOR = new RGB(0, 0, 128);
  private static final RGB DEFAULT_LINK_COLOR = new RGB(0, 0, 255);
  // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=155993
  private static final String UNIT;
  static {
    UNIT = Util.isMac() ? "px" : "pt"; //$NON-NLS-1$//$NON-NLS-2$
  }

  private Browser browser;
  private Color foreground;
  private Color background;
  @Nullable
  private Color linkColor;
  @Nullable
  private Color activeLinkColor;
  private Font defaultFont;
  private final boolean useEditorFontSize;
  private String htmlBody = "";
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> scheduledFuture;
  private final Label labelToCopyColorsFrom;

  public SonarLintWebView(Composite parent, boolean useEditorFontSize) {
    super(parent, SWT.NONE);
    var layout = new GridLayout();
    layout.horizontalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    setLayout(layout);
    this.labelToCopyColorsFrom = new Label(this, SWT.LEFT);
    labelToCopyColorsFrom.setVisible(false);
    var layoutData = new GridData();
    layoutData.exclude = true;
    labelToCopyColorsFrom.setLayoutData(layoutData);
    this.useEditorFontSize = useEditorFontSize;
    try {
      browser = new Browser(this, SWT.NONE);
      BrowserUtils.addLinkListener(browser);
      var browserLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
      browser.setLayoutData(browserLayoutData);
      browser.setJavascriptEnabled(true);
      // Cancel opening of new windows
      browser.addOpenWindowListener(event -> event.required = true);
      // Replace browser's built-in context menu with none
      browser.setMenu(new Menu(parent.getShell(), SWT.NONE));

      cacheColorsAndFonts();

      parent.addListener(SWT.Paint, this);
      JFaceResources.getColorRegistry().addListener(this);
      JFaceResources.getFontRegistry().addListener(this);

      addDisposeListener(e -> {
        scheduler.shutdownNow();
        JFaceResources.getColorRegistry().removeListener(this);
        JFaceResources.getFontRegistry().removeListener(this);
        parent.removeListener(SWT.Paint, this);
      });

      // Hide the browser while resizing to avoid the lag
      addControlListener(new ControlAdapter() {

        @Override
        public void controlResized(ControlEvent e) {
          SonarLintWebView.this.setVisible(false);
          if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
          }
          scheduledFuture = scheduler.schedule(
            () -> browser.getDisplay().asyncExec(() -> {
              updateBrowserHeightHint(browserLayoutData);
              SonarLintWebView.this.setVisible(true);
            }),
            300, TimeUnit.MILLISECONDS);
        }
      });

      browser.addProgressListener(ProgressListener.completedAdapter(event -> {
        updateBrowserHeightHint(browserLayoutData);
      }));
      // This is to avoid the browser to capture mouse wheel events, and so preventing to scroll the parent scrollable
      browser.setCapture(false);
      browser.setEnabled(false);
    } catch (SWTError e) {
      // Browser is probably not available but it will be partially initialized
      for (var c : parent.getChildren()) {
        if (c instanceof Browser) {
          c.dispose();
        }
      }
      new Label(parent, SWT.WRAP).setText("Unable to create SWT Browser:\n " + e.getMessage());
    }
    reload();
  }

  private void updateBrowserHeightHint(GridData browserLayoutData) {
    try {
      var height = (Double) browser.evaluate("return document.body.scrollHeight;"); //$NON-NLS-1$
      browserLayoutData.heightHint = height.intValue();
      browser.requestLayout();
    } catch (SWTException ignored) {
      // When switching fast between rules, the browser evaluation takes too long and the methods
      // afterwards try to run against a disposed widget!
    }
  }

  @Override
  public void handleEvent(Event event) {
    updateColorAndFontCache();
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    updateColorAndFontCache();
  }

  private Font getDefaultFont() {
    if (useEditorFontSize) {
      return JFaceResources.getTextFont();
    } else {
      return labelToCopyColorsFrom.getFont();
    }
  }

  private void updateColorAndFontCache() {
    var shouldRefresh = cacheColorsAndFonts();
    if (shouldRefresh) {
      // Reload HTML to possibly apply theme change
      browser.getDisplay().asyncExec(() -> {
        if (!browser.isDisposed()) {
          reload();
        }
      });
    }
  }

  private boolean cacheColorsAndFonts() {
    var changed = false;
    if (!getDefaultFont().equals(defaultFont)) {
      this.defaultFont = getDefaultFont();
      changed = true;
    }
    var newFg = labelToCopyColorsFrom.getForeground();
    if (!Objects.equals(newFg, foreground)) {
      this.foreground = newFg;
      changed = true;
    }
    var newBg = labelToCopyColorsFrom.getBackground();
    if (!Objects.equals(newBg, background)) {
      this.background = newBg;
      changed = true;
    }
    var newLink = getLinkColor();
    if (!Objects.equals(newLink, linkColor)) {
      this.linkColor = newLink;
      changed = true;
    }
    var newActiveLink = getActiveLinkColor();
    if (!Objects.equals(newActiveLink, activeLinkColor)) {
      this.activeLinkColor = newActiveLink;
      changed = true;
    }
    return changed;
  }

  private String css() {
    var fontSizePt = defaultFont.getFontData()[0].getHeight();
    return "<style type=\"text/css\">"
      + "html, body {overflow-y:hidden;}"
      + "body { font-family: Helvetica Neue,Segoe UI,Helvetica,Arial,sans-serif; font-size: " + fontSizePt + UNIT + "; "
      + "color: " + hexColor(this.foreground) + ";background-color: " + hexColor(this.background)
      + ";}"
      + "h1 { margin-bottom: 0; font-size: 150% }"
      + "h1 .rulename { font-weight: bold; }"
      + "h1 .rulekey { font-weight: normal; font-size: smaller;}"
      + "h2 { font-size: 120% }"
      + "img { height: " + (fontSizePt + 1) + UNIT + "; width: " + (fontSizePt + 1) + UNIT + "; vertical-align: middle; }"
      + ".typeseverity span { font-size: 1em; margin-left: 0.5em; margin-right: 1em;}"
      + "div.typeseverity { padding: 0; margin: 0}"
      + "a { border-bottom: 1px solid " + hexColor(this.linkColor, DEFAULT_LINK_COLOR) + "; color: " + hexColor(this.linkColor)
      + "; cursor: pointer; outline: none; text-decoration: none;}"
      + "a:hover { color: " + hexColor(this.activeLinkColor, DEFAULT_ACTIVE_LINK_COLOR) + "}"
      + "code { padding: .2em .45em; margin: 0; background-color: " + hexColor(this.foreground, 50) + "; border-radius: 3px; white-space: nowrap; line-height: 1.6em}"
      + "pre { padding: .7em; border-top: 1px solid " + hexColor(this.foreground, 200) + "; border-bottom: 1px solid "
      + hexColor(this.foreground, 100)
      + "; overflow: auto;}"
      + "pre > code {padding: 0; background-color: transparent; white-space: pre; overflow-x: scroll;}"
      + "code, pre { font-family: Consolas,Liberation Mono,Menlo,Courier,monospace;}"
      + "ul { padding-left: 2.5em; list-style: disc;}"
      + ".rule-desc { line-height: 1.5em }"
      + "table.rule-params { line-height: 1em; }"
      + ".rule-params .param-description p { margin: 0.1em; }"
      + ".rule-params th { text-align: left; vertical-align: top }"
      + ".rule-params td { text-align: left; vertical-align: top }"
      + ".other-context-list {\n"
      + "  list-style: none;\n"
      + "}\n"
      + "\n"
      + ".other-context-list .do::before {\n"
      + "  content: \"\\2713\";\n"
      + "  color: green;\n"
      + "  padding-right:5px;\n"
      + "}\n"
      + "\n"
      + ".other-context-list .dont::before {\n"
      + "  content: \"\\2717\";\n"
      + "  color: red;\n"
      + "  padding-right:5px;\n"
      + "}"
      + "</style>";
  }

  @Nullable
  private Color getLinkColor() {
    return JFaceColors.getHyperlinkText(browser.getDisplay());
  }

  @Nullable
  private Color getActiveLinkColor() {
    return JFaceColors.getActiveHyperlinkText(browser.getDisplay());
  }

  public void setHtmlBody(String htmlBody) {
    this.htmlBody = htmlBody;
    if (browser == null) {
      return;
    }
    reload();
  }

  private void reload() {
    browser.setText("<!doctype html><html><head>" + css() + "</head><body>" + htmlBody + "</body></html>");
    browser.requestLayout();
  }

  private static String hexColor(@Nullable Color color, RGB defaultColor) {
    if (color != null) {
      return hexColor(color);
    }
    return "rgb(" + defaultColor.red + ", " + defaultColor.green + ", " + defaultColor.blue + ")";
  }

  private static String hexColor(Color color, Integer alpha) {
    return "rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + alpha / 255.0 + ")";
  }

  private static String hexColor(Color color) {
    return hexColor(color, getAlpha(color));
  }

  private static int getAlpha(Color c) {
    try {
      var m = Color.class.getMethod("getAlpha");
      return (int) m.invoke(c);
    } catch (Exception e) {
      return 255;
    }
  }

}
