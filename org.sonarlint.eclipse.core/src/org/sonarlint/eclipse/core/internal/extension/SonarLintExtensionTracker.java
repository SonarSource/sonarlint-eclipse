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
package org.sonarlint.eclipse.core.internal.extension;

import java.util.Collection;
import java.util.List;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileLanguageProvider;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider;
import org.sonarlint.eclipse.core.resource.IProjectScopeProvider;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectHierarchyProvider;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectsProvider;

public class SonarLintExtensionTracker extends AbstractSonarLintExtensionTracker {

  private static SonarLintExtensionTracker singleInstance = null;

  private final SonarLintEP<IAnalysisConfigurator> analysisEp = new SonarLintEP<>("org.sonarlint.eclipse.core.analysisConfigurator"); //$NON-NLS-1$
  private final SonarLintEP<ISonarLintProjectsProvider> projectsProviderEp = new SonarLintEP<>("org.sonarlint.eclipse.core.projectsProvider"); //$NON-NLS-1$
  private final SonarLintEP<ISonarLintFileAdapterParticipant> fileAdapterParticipantEp = new SonarLintEP<>("org.sonarlint.eclipse.core.fileAdapterParticipant"); //$NON-NLS-1$
  private final SonarLintEP<ISonarLintProjectAdapterParticipant> projectAdapterParticipantEp = new SonarLintEP<>(
    "org.sonarlint.eclipse.core.projectAdapterParticipant"); //$NON-NLS-1$
  private final SonarLintEP<IFileLanguageProvider> languageEp = new SonarLintEP<>("org.sonarlint.eclipse.core.languageProvider"); //$NON-NLS-1$
  private final SonarLintEP<IFileTypeProvider> typeEp = new SonarLintEP<>("org.sonarlint.eclipse.core.typeProvider"); //$NON-NLS-1$
  private final SonarLintEP<ISonarLintProjectHierarchyProvider> projectHierarchyProviderEP = new SonarLintEP<>(
    "org.sonarlint.eclipse.core.projectHierarchyProvider"); //$NON-NLS-1$
  private final SonarLintEP<IProjectScopeProvider> projectScopeProviderEP = new SonarLintEP<>(
    "org.sonarlint.eclipse.core.projectScopeProvider"); //$NON-NLS-1$

  private final Collection<SonarLintEP<?>> allEps = List.of(analysisEp, projectsProviderEp,
    fileAdapterParticipantEp, projectAdapterParticipantEp, languageEp, typeEp, projectHierarchyProviderEP,
    projectScopeProviderEP);

  private SonarLintExtensionTracker() {
    init(allEps);
  }

  public static synchronized SonarLintExtensionTracker getInstance() {
    if (singleInstance == null) {
      singleInstance = new SonarLintExtensionTracker();
    }
    return singleInstance;
  }

  public static void close() {
    if (singleInstance != null) {
      singleInstance.unregister();
    }
  }

  public Collection<IAnalysisConfigurator> getAnalysisConfigurators() {
    return analysisEp.getInstances();
  }

  public Collection<ISonarLintProjectsProvider> getProjectsProviders() {
    return projectsProviderEp.getInstances();
  }

  public Collection<ISonarLintProjectAdapterParticipant> getProjectAdapterParticipants() {
    return projectAdapterParticipantEp.getInstances();
  }

  public Collection<ISonarLintFileAdapterParticipant> getFileAdapterParticipants() {
    return fileAdapterParticipantEp.getInstances();
  }

  public Collection<IFileLanguageProvider> getLanguageProviders() {
    return languageEp.getInstances();
  }

  public Collection<IFileTypeProvider> getTypeProviders() {
    return typeEp.getInstances();
  }

  public Collection<ISonarLintProjectHierarchyProvider> getProjectHierarchyProviders() {
    return projectHierarchyProviderEP.getInstances();
  }

  public Collection<IProjectScopeProvider> getProjectScopeProviders() {
    return projectScopeProviderEP.getInstances();
  }
}
