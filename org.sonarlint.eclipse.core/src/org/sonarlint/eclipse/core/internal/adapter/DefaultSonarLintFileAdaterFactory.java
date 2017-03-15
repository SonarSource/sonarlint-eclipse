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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;

public class DefaultSonarLintFileAdaterFactory implements IAdapterFactory {

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (ISonarLintFile.class.equals(adapterType) && adaptableObject instanceof IAdaptable) {
      IResource resource = ((IAdaptable) adaptableObject).getAdapter(IResource.class);
      if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        return getAdapter(adapterType, file);
      }
    }
    return null;
  }

  private <T> T getAdapter(Class<T> adapterType, IFile file) {
    if (!SonarLintUtils.shouldAnalyze(file)) {
      return null;
    }
    Predicate<IFile> shouldExclude = SonarLintCorePlugin.getExtensionTracker().getFileAdapterParticipants().stream()
      .<Predicate<IFile>>map(participant -> participant::exclude)
      .reduce(Predicate::or)
      .orElse(x -> false);
    if (shouldExclude.test(file)) {
      return null;
    }
    for (ISonarLintFileAdapterParticipant p : SonarLintCorePlugin.getExtensionTracker().getFileAdapterParticipants()) {
      ISonarLintFile adapted = p.adapt(file);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    return adapterType.cast(new DefaultSonarLintFileAdapter(file));
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintFile.class};
  }

}
