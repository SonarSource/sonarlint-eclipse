package org.sonar.ide.eclipse.jdt.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.wsclient.services.Resource;

/**
 * Adapter factory for Java elements.
 */
@SuppressWarnings("unchecked")
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static Class[] ADAPTER_LIST = { Resource.class };

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adaptableObject instanceof IJavaProject) {
      IJavaProject javaProject = (IJavaProject) adaptableObject;
      String key = EclipseResourceUtils.getInstance().getProjectKey(javaProject.getResource());
      return new Resource().setKey(key);
    }
    return null;
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
