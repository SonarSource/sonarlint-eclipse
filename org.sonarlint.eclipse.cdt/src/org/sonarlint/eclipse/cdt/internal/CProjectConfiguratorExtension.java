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
import java.util.Optional;
import java.util.Set;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileLanguageProvider;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.rule.ISyntaxHighlightingProvider;
import org.sonarsource.sonarlint.core.commons.Language;

/**
 * Responsible for checking at runtime if CDT plugin is installed.
 */
public class CProjectConfiguratorExtension implements IAnalysisConfigurator, IFileLanguageProvider, ISyntaxHighlightingProvider {

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

  @Override
  public Set<String> whitelistedPlugins() {
    if (isCdtPresent()) {
      return Collections.singleton("cpp");
    }
    return Collections.emptySet();
  }

  @Override
  public Set<Language> whitelistedLanguages() {
    // Objective-C is not supported by CDT!
    return isCdtPresent() ? SonarLintUtils.CONNECTED_MODE_LANGUAGES_CDT : Collections.emptySet();
  }

  @Override
  public boolean canConfigure(ISonarLintProject project) {
    try {
      var underlyingProject = project.getResource() instanceof IProject ? (IProject) project.getResource() : null;
      // Constants are inlined so this should not cause ClassNotFound
      return cdtUtils != null &&
        underlyingProject != null &&
        (underlyingProject.hasNature(CProjectNature.C_NATURE_ID) || underlyingProject.hasNature(CCProjectNature.CC_NATURE_ID));
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return false;
    }
  }

  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    cdtUtils.configure(context, monitor);
  }

  @Nullable
  @Override
  public String language(ISonarLintFile file) {
    if (canConfigure(file.getProject())) {
      var iFile = file.getResource() instanceof IFile ? (IFile) file.getResource() : null;
      if (cdtUtils != null && iFile != null) {
        return cdtUtils.language(iFile);
      }
    }
    return null;
  }

  @Override
  public Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage) {
    if (isCdtPresent() && (ruleLanguage.equals(Language.C.getLanguageKey()) || ruleLanguage.equals(Language.CPP.getLanguageKey()))) {
      return Optional.of(CdtUiUtils.sourceViewerConfiguration());
    }
    return Optional.empty();
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    if (isCdtPresent() && (ruleLanguage.equals(Language.C.getLanguageKey()) || ruleLanguage.equals(Language.CPP.getLanguageKey()))) {
      return Optional.of(CdtUiUtils.documentPartitioner());
    }
    return Optional.empty();
  }
}
