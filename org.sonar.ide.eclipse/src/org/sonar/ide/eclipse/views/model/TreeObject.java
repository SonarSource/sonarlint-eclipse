package org.sonar.ide.eclipse.views.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;


/**
 * @author Jérémie Lagarde
 */
public abstract class TreeObject implements IAdaptable {

  private final Resource resource;
  private TreeParent parent;
  private TreePropertyProvider propertyProvider;

  public TreeObject(Resource resource) {
    this.resource = resource;
  }

  public String getName() {
    return resource.getName();
  }

  public abstract String getRemoteURL();

  protected String getRemoteRootURL() {
    return parent.getRemoteRootURL();
  }

  public String getVersion() {
    if (resource.getVersion() != null)
      return resource.getVersion();
    if (getParent() != null)
      return getParent().getVersion();
    return "";

  }

  public Resource getResource() {
    return resource;
  }

  public Sonar getServer() {
    return parent.getServer();
  }

  public void setParent(TreeParent parent) {
    this.parent = parent;
  }

  public TreeParent getParent() {
    return parent;
  }

  @Override
  public String toString() {
    return getName();
  }

  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (adapter == IPropertySource.class) {
      if (propertyProvider == null) {
        propertyProvider = new TreePropertyProvider(this);
      }
      return propertyProvider;
    }
    return null;
  }
}
