/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * Class used to initialize default preference values.
 *
 * @author Jérémie Lagarde
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = SonarPlugin.getDefault().getPreferenceStore();
    store.setDefault(PreferenceConstants.P_SONAR_SERVER_URL, PreferenceConstants.P_SONAR_SERVER_URL_DEFAULT);
    store.setDefault(PreferenceConstants.P_CONSOLE_REQUEST_COLOR, StringConverter.asString(new RGB(0, 255, 50)));
    store.setDefault(PreferenceConstants.P_CONSOLE_RESPONSE_COLOR, StringConverter.asString(new RGB(100, 100, 255)));
    store.setDefault(PreferenceConstants.P_CONSOLE_ERROR_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
  }

}
