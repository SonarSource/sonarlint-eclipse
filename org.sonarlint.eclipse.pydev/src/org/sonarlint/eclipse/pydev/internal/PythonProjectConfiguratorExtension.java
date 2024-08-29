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
package org.sonarlint.eclipse.pydev.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.IEditorPart;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.resource.IProjectScopeProvider;
import org.sonarlint.eclipse.ui.rule.ISyntaxHighlightingProvider;

public class PythonProjectConfiguratorExtension implements ISyntaxHighlightingProvider, IProjectScopeProvider {
  private static final String PYTHON_LANGUAGE_KEY = "py";
  private final boolean pyDevPresent;

  public PythonProjectConfiguratorExtension() {
    pyDevPresent = isPyDevPresent();
  }

  private static boolean isPyDevPresent() {
    return isClassPresentAtRuntime("org.python.pydev.plugin.PydevPlugin")
      && isClassPresentAtRuntime("org.python.pydev.core.CorePlugin");
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
  public Optional<SourceViewerConfiguration> sourceViewerConfiguration(String ruleLanguage) {
    if (pyDevPresent && ruleLanguage.equals(PYTHON_LANGUAGE_KEY)) {
      return Optional.of(PyDevUtils.sourceViewerConfiguration());
    }
    return Optional.empty();
  }

  @Nullable
  @Override
  public SonarLintLanguage getEditorLanguage(IEditorPart editor) {
    return pyDevPresent && PyDevUtils.isPythonEditor(editor)
      ? SonarLintLanguage.PYTHON
      : null;
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    if (pyDevPresent && ruleLanguage.equals(PYTHON_LANGUAGE_KEY)) {
      return Optional.of(PyDevUtils.documentPartitioner());
    }
    return Optional.empty();
  }

  /**
   *  This is independent from PyDev itself but linked here as it is for Python itself! Currently there is no direct
   *  way to read about virtual environments from
   */
  @Override
  public Set<IPath> getExclusions(IProject project) {
    var exclusions = new HashSet<IPath>();

    // Python virtual environments can be named optionally, therefore we only can "guess" the default names based on
    // different libraries / tools that create virtual environments. We will never catch all probably, but for users
    // naming their virtual environments differently that is their own "fault".
    // INFO: These is not the Java standard library PATH class, but the one from Eclipse that is called the same!
    exclusions.add(Path.fromOSString("/" + project.getName() + "/venv"));
    exclusions.add(Path.fromOSString("/" + project.getName() + "/pyenv"));
    exclusions.add(Path.fromOSString("/" + project.getName() + "/pyvenv"));
    exclusions.add(Path.fromOSString("/" + project.getName() + "/virtualenv"));

    return exclusions;
  }
}
