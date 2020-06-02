/*******************************************************************************
 * Copyright (c) 2004, 2013 Tasktop Technologies and others.
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

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * @author Steffen Pingel
 * @since 3.7
 */
public class CommonUiUtil {

    private static final String KEY_ENABLED = "org.eclipse.mylyn.commons.ui.enabled"; //$NON-NLS-1$

    public static void setMessage(DialogPage page, IStatus status) {
        String message = status.getMessage();
        switch (status.getSeverity()) {
        case IStatus.OK:
            page.setMessage(null, IMessageProvider.NONE);
            break;
        case IStatus.INFO:
            page.setMessage(message, IMessageProvider.INFORMATION);
            break;
        case IStatus.WARNING:
            page.setMessage(message, IMessageProvider.WARNING);
            break;
        default:
            page.setMessage(message, IMessageProvider.ERROR);
            break;
        }
    }

    /**
     * Recursively sets the menu of all children of <code>composite</code>.
     */
    public static void setMenu(Composite composite, Menu menu) {
        if (!composite.isDisposed()) {
            composite.setMenu(menu);
            for (Control child : composite.getChildren()) {
                child.setMenu(menu);
                if (child instanceof Composite) {
                    setMenu((Composite) child, menu);
                }
            }
        }
    }

    public static void setEnabled(Composite composite, boolean restore) {
        if (restore) {
            restoreState(composite);
        } else {
            saveStateAndDisable(composite);
        }
    }

    private static void saveStateAndDisable(Composite composite) {
        if (!composite.isDisposed()) {
            Object data = composite.getData(KEY_ENABLED);
            if (data == null) {
                if (!composite.getEnabled()) {
                    composite.setData(KEY_ENABLED, Boolean.FALSE);
                } else {
                    composite.setData(KEY_ENABLED, Boolean.TRUE);
                    composite.setEnabled(false);
                }
            }
            for (Control control : composite.getChildren()) {
                if (control instanceof Composite) {
                    saveStateAndDisable((Composite) control);
                } else {
                    data = control.getData(KEY_ENABLED);
                    if (data == null) {
                        if (!control.getEnabled()) {
                            control.setData(KEY_ENABLED, Boolean.FALSE);
                        } else {
                            control.setData(KEY_ENABLED, Boolean.TRUE);
                            control.setEnabled(false);
                        }
                    }
                }
            }
        }
    }

    private static void restoreState(Composite composite) {
        if (!composite.isDisposed()) {
            Object data = composite.getData(KEY_ENABLED);
            if (data != null) {
                if (data == Boolean.TRUE) {
                    composite.setEnabled(data == Boolean.TRUE);
                }
                composite.setData(KEY_ENABLED, null);
            }
            for (Control control : composite.getChildren()) {
                if (control instanceof Composite) {
                    restoreState((Composite) control);
                } else {
                    data = control.getData(KEY_ENABLED);
                    if (data != null) {
                        if (data == Boolean.TRUE) {
                            control.setEnabled(true);
                        }
                        control.setData(KEY_ENABLED, null);
                    }
                }
            }
        }
    }

    /**
     * Returns text masking the &amp;-character from decoration as an accelerator in SWT labels.
     * 
     * @deprecated use LegacyActionTools#escapeMnemonics(String) instead
     */
    @Deprecated
    public static String toLabel(String text) {
        return (text != null) ? text.replaceAll("&", "&&") : null; // mask & from SWT //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns text for use as the label of an action to ensure that it is displayed properly.
     * 
     * @return the cleaned text
     */
    public static String toMenuLabel(String label) {
        // a tab at the end of the text will make sure that the @ will not create a weird space in the action text
        // bug 287347: @ at start of task name cause a weird space in activation history menu
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=287347
        if (label.contains("@")) { //$NON-NLS-1$
            label += "\t"; //$NON-NLS-1$
        }
        return LegacyActionTools.escapeMnemonics(label);
    }

    public static String getProductName() {
        return getProductName(null);
    }

    public static String getProductName(String defaultName) {
        IProduct product = Platform.getProduct();
        if (product != null) {
            String productName = product.getName();
            if (productName != null) {
                String LABEL_SDK = "SDK"; //$NON-NLS-1$
                if (productName.endsWith(LABEL_SDK)) {
                    productName = productName.substring(0, productName.length() - LABEL_SDK.length()).trim();
                }
                return productName;
            }
        }
        return defaultName;
    }

}

