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

package org.sonar.ide.eclipse.views.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.wsclient.RemoteSonarUtils;
import org.sonar.wsclient.Host;

/**
 * @author Jérémie Lagarde
 */
public abstract class TreeObject implements IAdaptable {

  protected final SourceCode sourceCode;
  private TreeParent parent;
  private TreePropertyProvider propertyProvider;

  public TreeObject(SourceCode sourceCode) {
    this.sourceCode = sourceCode;
  }

  public String getName() {
    return sourceCode.getName();
  }

  public String getRemoteURL() {
    return RemoteSonarUtils.getUrl(sourceCode);
  }

  /**
   * TODO Godin: dosn't work since migration to new API
   */
  public String getVersion() {
    // if (resource.getVersion() != null) {
    // return resource.getVersion();
    // }
    if (getParent() != null) {
      return getParent().getVersion();
    }
    return "";
  }

  public SourceCode getSourceCode() {
    return sourceCode;
  }

  public Host getHost() {
    return parent.getHost();
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
