/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.pdt.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.rule.ISyntaxHighlightingProvider;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;
import org.sonarsource.sonarlint.core.commons.Language;

public class PHPProjectConfiguratorExtension
  implements IAnalysisConfigurator, ISonarLintFileAdapterParticipant, ISyntaxHighlightingProvider {
  private final boolean pdtPresent;
  private final boolean pdtUiPresent;

  public PHPProjectConfiguratorExtension() {
    pdtPresent = isPdtPresent();
    pdtUiPresent = isPdtUiPresent();
  }

  private static boolean isPdtPresent() {
    return isClassPresentAtRuntime("org.eclipse.php.internal.core.PHPCorePlugin");
  }

  private static boolean isPdtUiPresent() {
    return isClassPresentAtRuntime("org.eclipse.php.internal.ui.PHPUiPlugin");
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
  public Set<Language> whitelistedLanguages() {
    if (pdtPresent) {
      return EnumSet.of(Language.PHP);
    }
    return Collections.emptySet();
  }

  @Override
  public boolean canConfigure(ISonarLintProject project) {
    return pdtPresent && project.getResource() instanceof IProject
      && PdtUtils.hasPHPNature((IProject) project.getResource());
  }
  
  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    // TODO: Implement!
  }

  @Override
  public Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage) {
    if (pdtUiPresent && ruleLanguage.equals(Language.PHP.getLanguageKey())) {
      return Optional.of(PdtUiUtils.sourceViewerConfiguration());
    }
    return Optional.empty();
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    if (pdtUiPresent && ruleLanguage.equals(Language.PHP.getLanguageKey())) {
      return Optional.of(PdtUiUtils.documentPartitioner());
    }
    return Optional.empty();
  }
}
