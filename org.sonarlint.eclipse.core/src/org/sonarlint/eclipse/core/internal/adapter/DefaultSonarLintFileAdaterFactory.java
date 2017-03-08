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

public class DefaultSonarLintFileAdaterFactory implements IAdapterFactory {

  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarLintFile.class.equals(adapterType) && adaptableObject instanceof IAdaptable) {
      IResource resource = (IResource) ((IAdaptable) adaptableObject).getAdapter(IResource.class);
      if (resource instanceof IFile && SonarLintUtils.shouldAnalyze(resource)) {
        IFile file = (IFile) resource;
        Predicate<IFile> shouldKeep = SonarLintCorePlugin.getExtensionTracker().getFileFilters().stream()
          .<Predicate<IFile>>map(f -> f::test)
          .reduce(Predicate::and)
          .orElse(x -> true);
        if (shouldKeep.test(file)) {
          return adapterType.cast(new DefaultSonarLintFileAdapter(file));
        }
      }
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintFile.class};
  }

}
