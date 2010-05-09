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
          if (properties != null && properties.isProjectConfigured())
            return "true".equals(value);
        }
        return false;
      }

      public String getSonar() {
        return "true";
      }
    };
  }
}