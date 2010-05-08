/*
 * Copyright (C) 2010 Evgeny Mandrikov
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

package org.sonar.ide.eclipse.wizards;

import org.eclipse.ui.INewWizard;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

/**
 * @author Jérémie Lagarde
 */
public class NewServerLocationWizard extends AbstractServerLocationWizard implements INewWizard {

  protected String getTitle() {
    return Messages.getString("new.sonar.server"); //
  }

  protected String getDefaultUrl() {
    return SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
  }

}
