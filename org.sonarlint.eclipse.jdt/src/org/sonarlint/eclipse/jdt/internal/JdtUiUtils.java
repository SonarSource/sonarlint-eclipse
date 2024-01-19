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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.sonarlint.eclipse.core.internal.utils.CompatibilityUtils;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;

public class JdtUiUtils {
  public static ISonarLintMarkerResolver enhance(ISonarLintMarkerResolver resolution, IMarker marker) {
    if (CompatibilityUtils.supportMarkerResolutionRelevance()) {
      return new MarkerResolverRelevanceJdtAdapter(resolution, marker);
    } else {
      return new MarkerResolverJdtAdapter(resolution, marker);
    }
  }

  public static SourceViewerConfiguration sourceViewerConfiguration() {
    var tools = JavaPlugin.getDefault().getJavaTextTools();

    return new JavaSourceViewerConfiguration(tools.getColorManager(),
      JavaPlugin.getDefault().getCombinedPreferenceStore(), null, null);
  }

  public static IDocumentPartitioner documentPartitioner() {
    return new FastPartitioner(new FastJavaPartitionScanner(), new String[] {
      IJavaPartitions.JAVA_SINGLE_LINE_COMMENT, IJavaPartitions.JAVA_MULTI_LINE_COMMENT, IJavaPartitions.JAVA_DOC,
      IJavaPartitions.JAVA_STRING, IJavaPartitions.JAVA_CHARACTER, IJavaPartitions.JAVA_MULTI_LINE_STRING
    });
  }
}
