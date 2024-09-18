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
package org.sonarlint.eclipse.cdt.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileLanguageProvider;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.resource.IProjectScopeProvider;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.rule.ISyntaxHighlightingProvider;

/**
 * Responsible for checking at runtime if CDT plugin is installed.
 */
public class CProjectConfiguratorExtension implements IAnalysisConfigurator, IFileLanguageProvider,
  IProjectScopeProvider, ISyntaxHighlightingProvider {

  private static final String CPP_LANGUAGE_KEY = "cpp";
  private static final String C_LANGUAGE_KEY = "c";
  @Nullable
  private final CdtUtils cdtUtils;

  public CProjectConfiguratorExtension() {
    cdtUtils = isCdtPresent() ? new CdtUtils() : null;
  }

  private static boolean isCdtPresent() {
    try {
      Class.forName("org.eclipse.cdt.core.CCorePlugin");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean isCdtUiPresent() {
    try {
      Class.forName("org.eclipse.cdt.ui.CUIPlugin");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public Set<SonarLintLanguage> enableLanguages() {
    // Objective-C is not supported by CDT!
    return isCdtPresent() ? EnumSet.of(SonarLintLanguage.C, SonarLintLanguage.CPP) : Collections.emptySet();
  }

  @Override
  public boolean canConfigure(ISonarLintProject project) {
    var iProject = project.getResource() instanceof IProject ? (IProject) project.getResource() : null;
    return cdtUtils != null && iProject != null && cdtUtils.hasCOrCppNature(iProject);
  }

  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    cdtUtils.configure(context, monitor);
  }

  @Nullable
  @Override
  public SonarLintLanguage language(ISonarLintFile file) {
    if (canConfigure(file.getProject())) {
      var iFile = file.getResource() instanceof IFile ? (IFile) file.getResource() : null;
      if (cdtUtils != null && iFile != null) {
        return cdtUtils.language(iFile);
      }
    }
    return null;
  }

  /**
   *  We can only provide UI elements if the language matches C/C++ and the CDT UI plug-in is available as in some
   *  cases there might only be the core bundles present. This might be the case for thrid-party plug-ins that make
   *  use of the CDT core in order to work with C/C++ but provide their own "frontend".
   */
  private static boolean canProvideUiElements(String ruleLanguage) {
    return isCdtUiPresent() && (ruleLanguage.equals(C_LANGUAGE_KEY) || ruleLanguage.equals(CPP_LANGUAGE_KEY));
  }

  @Override
  public Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage) {
    return canProvideUiElements(ruleLanguage)
      ? Optional.of(CdtUiUtils.sourceViewerConfiguration())
      : Optional.empty();
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    return canProvideUiElements(ruleLanguage)
      ? Optional.of(CdtUiUtils.documentPartitioner())
      : Optional.empty();
  }

  @Nullable
  @Override
  public SonarLintLanguage getEditorLanguage(IEditorPart editor) {
    return isCdtUiPresent() && CdtUiUtils.isCEditor(editor)
      ? SonarLintLanguage.C
      : null;
  }

  @Nullable
  @Override
  public TextMergeViewer getTextMergeViewer(String ruleLanguage, Composite parent, CompareConfiguration mp) {
    return canProvideUiElements(ruleLanguage)
      ? CdtUiUtils.getTextMergeViewer(parent, mp)
      : null;
  }

  @Override
  public Set<IPath> getExclusions(IProject project) {
    return cdtUtils != null && cdtUtils.hasCOrCppNature(project)
      ? cdtUtils.getExcludedPaths(project)
      : Collections.emptySet();
  }
}
