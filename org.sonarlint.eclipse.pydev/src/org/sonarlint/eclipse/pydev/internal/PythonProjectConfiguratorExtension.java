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

import java.util.Optional;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.sonarlint.eclipse.core.rule.ISyntaxHighlightingProvider;
import org.sonarsource.sonarlint.core.commons.Language;

public class PythonProjectConfiguratorExtension implements ISyntaxHighlightingProvider {
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
    if (pyDevPresent && ruleLanguage.equals(Language.PYTHON.getLanguageKey())) {
      return Optional.of(PyDevUtils.sourceViewerConfiguration());
    }
    return Optional.empty();
  }

  @Override
  public Optional<IDocumentPartitioner> documentPartitioner(String ruleLanguage) {
    if (pyDevPresent && ruleLanguage.equals(Language.PYTHON.getLanguageKey())) {
      return Optional.of(PyDevUtils.documentPartitioner());
    }
    return Optional.empty();
  }
}
