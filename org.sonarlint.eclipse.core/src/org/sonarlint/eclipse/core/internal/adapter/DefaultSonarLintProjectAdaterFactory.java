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
import org.sonarlint.eclipse.core.resource.ISonarLintProjectAdapterParticipant;

public class DefaultSonarLintProjectAdaterFactory implements IAdapterFactory {

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (ISonarLintProject.class.equals(adapterType) && adaptableObject instanceof IAdaptable) {
      IProject project = ((IAdaptable) adaptableObject).getAdapter(IProject.class);
      if (project != null) {
        return getAdapter(adapterType, project);
      }
    }
    return null;
  }

  private <T> T getAdapter(Class<T> adapterType, IProject project) {
    Predicate<IProject> shouldExclude = SonarLintCorePlugin.getExtensionTracker().getProjectAdapterParticipants().stream()
      .<Predicate<IProject>>map(participant -> participant::exclude)
      .reduce(Predicate::or)
      .orElse(x -> false);
    if (shouldExclude.test(project)) {
      return null;
    }
    for (ISonarLintProjectAdapterParticipant p : SonarLintCorePlugin.getExtensionTracker().getProjectAdapterParticipants()) {
      ISonarLintProject adapted = p.adapt(project);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    return adapterType.cast(new DefaultSonarLintProjectAdapter(project));
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintProject.class};
  }

}
