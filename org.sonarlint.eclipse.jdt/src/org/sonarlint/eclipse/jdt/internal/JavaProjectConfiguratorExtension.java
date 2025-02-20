/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.resource.IProjectScopeProvider;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.quickfixes.IMarkerResolutionEnhancer;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;
import org.sonarlint.eclipse.ui.rule.ISyntaxHighlightingProvider;

public class JavaProjectConfiguratorExtension
  implements IAnalysisConfigurator, ISonarLintFileAdapterParticipant, IFileTypeProvider, IMarkerResolutionEnhancer,
  IProjectScopeProvider, ISyntaxHighlightingProvider {

  private static final String JAVA_LANGUAGE_KEY = "java";
  @Nullable
  private final JdtUtils javaProjectConfigurator;
  private final boolean jdtPresent;
  private final boolean jdtUiPresent;

  public JavaProjectConfiguratorExtension() {
    jdtPresent = isJdtPresent();
    jdtUiPresent = isJdtUiPresent();
    javaProjectConfigurator = jdtPresent ? new JdtUtils() : null;
  }

  private static boolean isJdtPresent() {
    return isClassPresentAtRuntime("org.eclipse.jdt.core.JavaCore");
  }

  private static boolean isJdtUiPresent() {
    return isClassPresentAtRuntime("org.eclipse.jdt.ui.text.java.IJavaCompletionProposal");
  }

  private static boolean isClassPresentAtRuntime(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public Set<SonarLintLanguage> enableLanguages() {
    return isJdtPresent() ? EnumSet.of(SonarLintLanguage.JAVA, SonarLintLanguage.JSP) : Collections.emptySet();
  }

  @Override
  public boolean canConfigure(ISonarLintProject project) {
    return jdtPresent && project.getResource() instanceof IProject
      && JdtUtils.hasJavaNature((IProject) project.getResource());
  }

  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    javaProjectConfigurator.configure(context, monitor);
  }

  @Override
  public boolean exclude(IFile file) {
    if (jdtPresent) {
      return JdtUtils.shouldExclude(file);
    }
    return false;
  }

  @Override
  public ISonarLintFileType qualify(ISonarLintFile file) {
    if (jdtPresent) {
      return JdtUtils.qualify(file);
    }
    return ISonarLintFileType.UNKNOWN;
  }

  @Override
  public ISonarLintMarkerResolver enhance(ISonarLintMarkerResolver resolution, IMarker marker) {
    if (jdtUiPresent) {
      return JdtUiUtils.enhance(resolution, marker);
    }
    return resolution;
  }

  @Override
  public Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage) {
    if (jdtUiPresent && ruleLanguage.equals(JAVA_LANGUAGE_KEY)) {
      return Optional.of(JdtUiUtils.sourceViewerConfiguration());
    }
    return Optional.empty();
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    if (jdtUiPresent && ruleLanguage.equals(JAVA_LANGUAGE_KEY)) {
      return Optional.of(JdtUiUtils.documentPartitioner());
    }
    return Optional.empty();
  }

  @Nullable
  @Override
  public SonarLintLanguage getEditorLanguage(IEditorPart editor) {
    return jdtUiPresent && JdtUiUtils.isJavaEditor(editor)
      ? SonarLintLanguage.JAVA
      : null;
  }

  @Nullable
  @Override
  public TextMergeViewer getTextMergeViewer(String ruleLanguage, Composite parent, CompareConfiguration mp) {
    if (jdtUiPresent && ruleLanguage.equals(JAVA_LANGUAGE_KEY)) {
      return JdtUiUtils.getTextMergeViewer(parent, mp);
    }
    return null;
  }

  @Override
  public Set<IPath> getExclusions(IProject project) {
    return jdtPresent && JdtUtils.hasJavaNature(project)
      ? JdtUtils.getExcludedPaths(project)
      : Collections.emptySet();
  }
}
