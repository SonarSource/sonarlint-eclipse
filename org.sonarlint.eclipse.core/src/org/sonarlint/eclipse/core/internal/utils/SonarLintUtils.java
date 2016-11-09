/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class SonarLintUtils {

  private SonarLintUtils() {
    // utility class, forbidden constructor
  }

  /**
   * Remove files that are already included in a more specific fileset.
   *
   * @param filesets filesets per project
   */
  public static void removeAggregatedDuplicates(Map<IProject, Collection<IFile>> changedFilesPerProject) {
    List<Map.Entry<IProject, Collection<IFile>>> entries = new ArrayList<>(changedFilesPerProject.entrySet());
    for (int i = 0; i < entries.size() - 1; i++) {
      for (int j = i + 1; j < entries.size(); j++) {
        IProject p1 = entries.get(i).getKey();
        Collection<IFile> fileset1 = entries.get(i).getValue();

        IProject p2 = entries.get(j).getKey();
        Collection<IFile> fileset2 = entries.get(j).getValue();

        if (isParentOf(p1, p2)) {
          removeAggregatedDuplicates(fileset1, fileset2);
        } else if (isParentOf(p2, p1)) {
          removeAggregatedDuplicates(fileset2, fileset1);
        }
      }
    }
  }

  private static void removeAggregatedDuplicates(Collection<IFile> parentset, Collection<IFile> childset) {
    Map<IPath, IFile> map = parentset.stream().collect(Collectors.toMap(IFile::getLocation, Function.identity()));
    for (IFile child : childset) {
      IFile parent = map.get(child.getLocation());
      if (parent != null) {
        parentset.remove(parent);
      }
    }
  }

  public static boolean isParentOf(IProject maybeParent, IProject maybeChild) {
    return maybeParent.getLocation().isPrefixOf(maybeChild.getLocation());
  }
}
