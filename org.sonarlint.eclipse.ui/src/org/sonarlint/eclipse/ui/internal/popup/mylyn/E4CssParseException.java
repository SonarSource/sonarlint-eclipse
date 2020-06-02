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

public class E4CssParseException extends Exception {
    private static final long serialVersionUID = 6799939105221602854L;

    public E4CssParseException(String type, String value) {
        super("Cannot parse " + type + " value from :" + value); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
