/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
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

import java.lang.reflect.Field;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Provides commons fonts. Fields may by <code>null</code> until initialization is completed.
 * <p>
 * NOTE: Use of this class is discouraged. Fonts are static and not updated when system or theme settings change.
 * </p>
 * 
 * @author Mik Kersten
 * @since 3.7
 */
public class CommonFonts {

    public static Font BOLD;

    public static Font ITALIC;

    public static Font BOLD_ITALIC;

    public static Font STRIKETHROUGH = null;

    public static boolean HAS_STRIKETHROUGH;

    static {
        if (Display.getCurrent() != null) {
            init();
        } else {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    init();
                }
            });
        }
    }

    private static void init() {
        BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
        ITALIC = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
        BOLD_ITALIC = new Font(Display.getCurrent(), getModifiedFontData(ITALIC.getFontData(), SWT.BOLD | SWT.ITALIC));

        Font defaultFont = JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
        FontData[] defaultData = defaultFont.getFontData();
        if (defaultData != null && defaultData.length == 1) {
            FontData data = new FontData(defaultData[0].getName(), defaultData[0].getHeight(),
                    defaultData[0].getStyle());

            if ("win32".equals(SWT.getPlatform())) { //$NON-NLS-1$
                // NOTE: Windows only, for: data.data.lfStrikeOut = 1;
                try {
                    Field dataField = data.getClass().getDeclaredField("data"); //$NON-NLS-1$
                    Object dataObject = dataField.get(data);
                    Class<?> clazz = dataObject.getClass().getSuperclass();
                    Field strikeOutFiled = clazz.getDeclaredField("lfStrikeOut"); //$NON-NLS-1$
                    strikeOutFiled.set(dataObject, (byte) 1);
                    CommonFonts.STRIKETHROUGH = new Font(Display.getCurrent(), data);
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        if (CommonFonts.STRIKETHROUGH == null) {
            CommonFonts.HAS_STRIKETHROUGH = false;
            CommonFonts.STRIKETHROUGH = defaultFont;
        } else {
            CommonFonts.HAS_STRIKETHROUGH = true;
        }
    }

    /**
     * NOTE: disposal of JFaceResources fonts handled by registry.
     */
    public static void dispose() {
        if (CommonFonts.STRIKETHROUGH != null && !CommonFonts.STRIKETHROUGH.isDisposed()) {
            CommonFonts.STRIKETHROUGH.dispose();
            CommonFonts.BOLD_ITALIC.dispose();
        }
    }

    /**
     * Copied from {@link FontRegistry}
     */
    private static FontData[] getModifiedFontData(FontData[] baseData, int style) {
        FontData[] styleData = new FontData[baseData.length];
        for (int i = 0; i < styleData.length; i++) {
            FontData base = baseData[i];
            styleData[i] = new FontData(base.getName(), base.getHeight(), base.getStyle() | style);
        }

        return styleData;
    }

}
