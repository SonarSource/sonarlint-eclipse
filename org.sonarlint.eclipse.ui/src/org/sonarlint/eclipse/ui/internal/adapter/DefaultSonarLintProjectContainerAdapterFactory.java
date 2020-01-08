/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.adapter;

import java.util.ArrayList;
import java.util.Collection;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IWorkingSet;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectContainer;

public class DefaultSonarLintProjectContainerAdapterFactory implements IAdapterFactory {

  private static final class DefaultSonarLintProjectContainer implements ISonarLintProjectContainer {
    private final IWorkingSet workingSet;

    public DefaultSonarLintProjectContainer(IWorkingSet workingSet) {
      this.workingSet = workingSet;
    }

    @Override
    public Collection<ISonarLintProject> projects() {
      Collection<ISonarLintProject> result = new ArrayList<>();
      for (IAdaptable elem : workingSet.getElements()) {
        ISonarLintProject p = Adapters.adapt(elem, ISonarLintProject.class);
        if (p != null) {
          result.add(p);
          continue;
        }
        ISonarLintProjectContainer c = Adapters.adapt(elem, ISonarLintProjectContainer.class);
        if (c != null) {
          result.addAll(c.projects());
        }
      }
      return result;
    }
  }

  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarLintProjectContainer.class.equals(adapterType) && adaptableObject instanceof IWorkingSet) {
      return adapterType.cast(new DefaultSonarLintProjectContainer((IWorkingSet) adaptableObject));
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintProjectContainer.class};
  }

}
