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

package org.sonar.ide.eclipse.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;
import org.sonar.ide.eclipse.properties.ProjectProperties;

/**
 * @author Jérémie Lagarde
 */
public class SonarAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_TYPES = new Class[] { IActionFilter.class };
  private static final String  SONAR_PROJECT = "sonar";

  public Class[] getAdapterList() {
    return ADAPTER_TYPES;
  }

  public Object getAdapter(final Object adaptable, Class adapterType) {
    return new IActionFilter() {
      public boolean testAttribute(Object target, String name, String value) {
        if (SONAR_PROJECT.equals(name) && adaptable instanceof IResource) {
          final ProjectProperties properties = ProjectProperties.getInstance((IResource) adaptable);
          if (properties != null && properties.isProjectConfigured()) {
            return "true".equals(value);
          }
        }
        return false;
      }

      public String getSonar() {
        return "true";
      }
    };
  }
}
