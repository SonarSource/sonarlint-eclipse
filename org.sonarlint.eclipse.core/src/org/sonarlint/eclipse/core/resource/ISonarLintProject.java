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
package org.sonarlint.eclipse.core.resource;

import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;

/**
 * Represents a project for SonarLint. A project will contain the 
 * project level specific SonarLint configuration (binding, exclusions, ...).
 * @since 2.7
 */
public interface ISonarLintProject extends ISonarLintIssuable {

  /**
   * Is the project open. Most actions are disabled on closed projects.
   */
  boolean isOpen();

  /**
   * Is this project already bound to a remote SonarQube project/module?
   */
  default boolean isBound() {
    return SonarLintProjectConfiguration.read(getScopeContext()).isBound();
  }

  /**
   * Is auto-analysis enabled on this project (ie on save / on open)
   */
  default boolean isAutoEnabled() {
    return SonarLintProjectConfiguration.read(getScopeContext()).isAutoEnabled();
  }

  /**
   * The scope context used to store SonarLint configuration
   */
  IScopeContext getScopeContext();

  /**
   * Unique name of the project that is displayed in logs and UI
   */
  @Override
  String getName();

  /**
   * Physical directory that is used to compute relative path for files to be analyzed.
   * TODO we should get rid of this at some point.
   */
  Path getBaseDir();

  /**
   * Some analyzers will report global issues that are not specific to a single file. Such issues
   * will have their marker attached to a parent resource (usually the project containing the file).
   */
  IResource getResourceForProjectLevelIssues();

  /**
   * Working directory for analyzers (they may store temporary files for example).
   */
  Path getWorkingDir();

  /**
   * Verify existence of a file in the current project using relative path
   */
  boolean exists(String relativeFilePath);

  /**
   * Object to be notified when SonarLintProjectDecorator (the wave overlay on projects) should be updated.
   * Default implementation returns the corresponding IProject
   */
  Object getObjectToNotify();

  /**
   * List of files in this project reported as changed by the SCM (ie that contains local changes).
   */
  Collection<ISonarLintFile> getScmChangedFiles(IProgressMonitor monitor);

  /**
   * Return the reason why this project doesn't support SCM feature. This will be displayed to the user.
   * @return <code>null</code> if SCM is supported.
   */
  @CheckForNull
  String getNoScmReason();

  /**
   * @return all SonarLint files contained in this project.
   */
  Collection<ISonarLintFile> files();

  /**
   * Return the underlying IProject when possible. Will be used by caller to access
   * project specific informations, like nature, ...
   * Caller should be ready to handle null values.
   */
  @CheckForNull
  IProject getUnderlyingProject();

}
