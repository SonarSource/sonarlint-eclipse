/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.ArrayList;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * A handler for a command on an issue
 */
public abstract class AbstractIssueCommand extends AbstractHandler {

  public Display getDisplay() {
    var display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  @Nullable
  protected static IMarker getSelectedMarker(IStructuredSelection selection) {
    var selectedSonarMarkers = new ArrayList<IMarker>();

    var elems = selection.toList();
    for (var elem : elems) {
      var marker = SonarLintUtils.adapt(elem, IMarker.class,
        "[AbstractIssueCommand#getSelectedMarker] Try get marker of object '" + elem + "'");
      if (marker != null) {
        selectedSonarMarkers.add(marker);
      }
    }
    return !selectedSonarMarkers.isEmpty() ? selectedSonarMarkers.get(0) : null;
  }

  /** Check for issue binding: Either SonarQube or SonarCloud */
  protected static Optional<ResolvedBinding> getBinding(IMarker marker) {
    var slFile = SonarLintUtils.adapt(marker.getResource(), ISonarLintFile.class,
      "[AbstractIssueCommand#getBinding] Try get file of marker '" + marker + "'");
    if (slFile == null) {
      return Optional.empty();
    }

    return SonarLintCorePlugin.getConnectionManager().resolveBinding(slFile.getProject());
  }

  @Nullable
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    var marker = getSelectedMarker((IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event));
    if (marker != null) {
      execute(marker, HandlerUtil.getActiveWorkbenchWindowChecked(event));
    }
    return null;
  }

  protected abstract void execute(IMarker selectedMarker, IWorkbenchWindow window);

}
