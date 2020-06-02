/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.sonarlint.eclipse.ui.internal.popup.mylyn;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;

public class E4ThemeColor {

    private static boolean loggedError = false;

    public static RGB getRGBFromCssString(String cssValue) {
        try {
            if (cssValue.startsWith("rgb(")) { //$NON-NLS-1$
                String rest = cssValue.substring(4, cssValue.length());
                int idx = rest.indexOf("rgb("); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(idx + 4, rest.length());
                }
                idx = rest.indexOf(")"); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(0, idx);
                }
                String[] rgbValues = rest.split(","); //$NON-NLS-1$
                if (rgbValues.length == 3) {
                    return new RGB(Integer.parseInt(rgbValues[0].trim()), Integer.parseInt(rgbValues[1].trim()),
                            Integer.parseInt(rgbValues[2].trim()));
                }
            } else if (cssValue.startsWith("#")) { //$NON-NLS-1$
                String rest = cssValue.substring(1, cssValue.length());
                int idx = rest.indexOf("#"); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(idx + 1, rest.length());
                }
                if (rest.length() > 5) {
                    return new RGB(Integer.parseInt(rest.substring(0, 2), 16),
                            Integer.parseInt(rest.substring(2, 4), 16), Integer.parseInt(rest.substring(4, 6), 16));
                }
            }
            throw new E4CssParseException("RGB", cssValue); //$NON-NLS-1$
        } catch (NumberFormatException | E4CssParseException e) {
            logOnce(e);
            return null;
        }
    }

    public static String getCssValueFromTheme(Display display, String value) {
      BundleContext context = FrameworkUtil.getBundle(E4ThemeColor.class).getBundleContext();
      try {
        ServiceReference<IThemeManager> reference = context.getServiceReference(IThemeManager.class);
        if (reference != null) {
          IThemeManager iThemeManager = context.getService(reference); //$NON-NLS-1$
          if (iThemeManager != null) {
            IThemeEngine themeEngine = iThemeManager.getEngineForDisplay(display); //$NON-NLS-1$
            if (themeEngine != null) {
              CSSStyleDeclaration shellStyle = getStyleDeclaration(themeEngine, display);
              if (shellStyle != null) {
                CSSValue cssValue = shellStyle.getPropertyCSSValue(value);
                if (cssValue != null) {
                  return cssValue.getCssText();
                }
              }
            }
          }
        }
      }
      catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        logOnce(e);
        return null;
      }
      return null;
    }

    private static CSSStyleDeclaration getStyleDeclaration(IThemeEngine themeEngine, Display display)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Shell shell = display.getActiveShell();
        CSSStyleDeclaration shellStyle = null;
        if (shell != null) {
            shellStyle = retrieveStyleFromShell(themeEngine, shell);
        } else {
            for (Shell input : display.getShells()) {
                shellStyle = retrieveStyleFromShell(themeEngine, input);
                if (shellStyle != null) {
                    break;
                }
            }
        }
        return shellStyle;
    }

    private static CSSStyleDeclaration retrieveStyleFromShell(IThemeEngine themeEngine, Shell shell)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object shellStyle = themeEngine.getStyle(shell); //$NON-NLS-1$
        if (shellStyle instanceof CSSStyleDeclaration) {
            return (CSSStyleDeclaration) shellStyle;
        }
        return null;
    }

    private static void logOnce(Exception e) {
        if (!loggedError) {
            SonarLintLogger.get().error(e.getMessage(), e);
            loggedError = true;
        }
    }
}
