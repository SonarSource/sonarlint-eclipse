/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;

public class SonarLintDebugResourceChangedListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    var res = event.getResource();
    try {
      switch (event.getType()) {
        case IResourceChangeEvent.PRE_CLOSE:
          SonarLintLogger.get().info("Project " + res.getFullPath() + " is about to close.");
          break;
        case IResourceChangeEvent.PRE_DELETE:
          SonarLintLogger.get().info("Project " + res.getFullPath() + " is about to be deleted.");
          break;
        case IResourceChangeEvent.POST_CHANGE:
          SonarLintLogger.get().info("Resources have changed.");
          event.getDelta().accept(new DeltaPrinter());
          break;
        case IResourceChangeEvent.PRE_BUILD:
          SonarLintLogger.get().info("Build about to run.");
          event.getDelta().accept(new DeltaPrinter());
          break;
        case IResourceChangeEvent.POST_BUILD:
          SonarLintLogger.get().info("Build complete.");
          event.getDelta().accept(new DeltaPrinter());
          break;
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to process resource changed event", e);
    }
  }

  static class DeltaPrinter implements IResourceDeltaVisitor {
    @Override
    public boolean visit(IResourceDelta delta) {
      var res = delta.getResource();
      switch (delta.getKind()) {
        case IResourceDelta.ADDED:
          SonarLintLogger.get().info("  Resource " + res.getFullPath() + " was added.");
          break;
        case IResourceDelta.REMOVED:
          SonarLintLogger.get().info("  Resource " + res.getFullPath() + " was removed.");
          break;
        case IResourceDelta.CHANGED:
          SonarLintLogger.get().info("  Resource " + res.getFullPath() + " has changed.");
          var flags = delta.getFlags();
          if ((flags & IResourceDelta.CONTENT) != 0) {
            SonarLintLogger.get().info("    --> Content Change");
          }
          if ((flags & IResourceDelta.REPLACED) != 0) {
            SonarLintLogger.get().info("    --> Content Replaced");
          }
          break;
      }
      // visit the children
      return true;
    }
  }

}
