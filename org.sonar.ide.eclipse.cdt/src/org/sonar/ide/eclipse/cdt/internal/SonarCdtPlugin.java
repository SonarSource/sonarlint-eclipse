/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.cdt.internal;

import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.AbstractPlugin;

public class SonarCdtPlugin extends AbstractPlugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.cdt"; //$NON-NLS-1$

  private static SonarCdtPlugin plugin;

  public SonarCdtPlugin() {
    plugin = this;
  }

  /**
   * @return the shared instance
   */
  public static SonarCdtPlugin getDefault() {
    return plugin;
  }

  @Override
  public void start(BundleContext context) {
    super.start(context);
    LoggerFactory.getLogger(getClass()).debug("SonarCdtPlugin started");
  }

  @Override
  public void stop(BundleContext context) {
    super.stop(context);
    LoggerFactory.getLogger(getClass()).debug("SonarCdtPlugin stopped");
  }

  static String getRelativePath(IPath rootPath, IPath path) {
    return path.makeRelativeTo(rootPath).toOSString();
  }

  /**
   * COPIED from {org.eclipse.cdt.internal.ui.wizards.classwizard.NewClassWizardUtil}
   * Returns the parent source folder of the given element. If the given
   * element is already a source folder, the element itself is returned.
   *
   * @param element the C Element
   * @return the source folder
   */
  public static ICContainer getSourceFolder(ICElement element) {
    ICContainer folder = null;
    boolean foundSourceRoot = false;
    ICElement curr = element;
    while (curr != null && !foundSourceRoot) {
      if (curr instanceof ICContainer && folder == null) {
        folder = (ICContainer) curr;
      }
      foundSourceRoot = (curr instanceof ISourceRoot);
      curr = curr.getParent();
    }
    if (folder == null) {
      ICProject cproject = element.getCProject();
      folder = cproject.findSourceRoot(cproject.getProject());
    }
    return folder;
  }

}
