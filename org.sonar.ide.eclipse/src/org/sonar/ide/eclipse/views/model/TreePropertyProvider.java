package org.sonar.ide.eclipse.views.model;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.sonar.ide.eclipse.Messages;

/**
 * @author Jérémie Lagarde
 */
public class TreePropertyProvider implements IPropertySource {

  private final TreeObject node;

  public TreePropertyProvider(TreeObject node) {
    this.node = node;
  }

  public Object getEditableValue() {
    return null;
  }

  public IPropertyDescriptor[] getPropertyDescriptors() {
    return new IPropertyDescriptor[]{
        new TextPropertyDescriptor("id", Messages.getString("prop.resource.id")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("key", Messages.getString("prop.resource.key")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("language", Messages.getString("prop.resource.language")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("longname", Messages.getString("prop.resource.longname")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("name", Messages.getString("prop.resource.name")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("qualifier", Messages.getString("prop.resource.qualifier")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("scope", Messages.getString("prop.resource.scope")), //$NON-NLS-1$ //$NON-NLS-2$
        new TextPropertyDescriptor("version", Messages.getString("prop.resource.version")), //$NON-NLS-1$ //$NON-NLS-2$
    };
  }

  public Object getPropertyValue(Object id) {
    if (id.equals("id")) { //$NON-NLS-1$
      return node.getResource().getId();
    }
    if (id.equals("key")) { //$NON-NLS-1$
      return node.getResource().getKey();
    }
    if (id.equals("language")) { //$NON-NLS-1$
      return node.getResource().getLanguage();
    }
    if (id.equals("longname")) { //$NON-NLS-1$
      return node.getResource().getLongName();
    }
    if (id.equals("name")) { //$NON-NLS-1$
      return node.getResource().getName();
    }
    if (id.equals("qualifier")) { //$NON-NLS-1$
      return node.getResource().getQualifier();
    }
    if (id.equals("scope")) { //$NON-NLS-1$
      return node.getResource().getScope();
    }
    if (id.equals("version")) { //$NON-NLS-1$
      return node.getVersion();
    }

    return ""; //$NON-NLS-1$
  }

  public boolean isPropertySet(Object id) {
    // Sonar Resource properties are read-only, so do nothing
    return false;
  }

  public void resetPropertyValue(Object id) {
    // Sonar Resource properties are read-only, so do nothing
  }

  public void setPropertyValue(Object id, Object value) {
    // Sonar Resource properties are read-only, so do nothing
  }
}
