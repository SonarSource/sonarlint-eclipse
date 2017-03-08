/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.adapter;

import java.util.function.Predicate;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class DefaultSonarLintProjectAdaterFactory implements IAdapterFactory {

  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarLintProject.class.equals(adapterType) && adaptableObject instanceof IAdaptable) {
      IProject project = (IProject) ((IAdaptable) adaptableObject).getAdapter(IProject.class);
      if (project != null) {
        Predicate<IProject> shouldKeep = SonarLintCorePlugin.getExtensionTracker().getProjectFilters().stream()
          .<Predicate<IProject>>map(f -> f::test)
          .reduce(Predicate::and)
          .orElse(x -> true);
        if (shouldKeep.test(project)) {
          return adapterType.cast(new DefaultSonarLintProjectAdapter(project));
        }
      }
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintProject.class};
  }

}
