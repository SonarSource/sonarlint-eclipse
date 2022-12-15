/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessClient;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.popup.BindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.popup.SingleBindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.BindingSuggestionDto;
import org.sonarsource.sonarlint.core.clientapi.client.OpenUrlInBrowserParams;
import org.sonarsource.sonarlint.core.clientapi.client.SuggestBindingParams;

public class SonarLintEclipseClient extends SonarLintEclipseHeadlessClient {

  @Override
  public void openUrlInBrowser(OpenUrlInBrowserParams params) {
    BrowserUtils.openExternalBrowser(params.getUrl());
  }

  @Override
  public void suggestBinding(SuggestBindingParams params) {
    Map<ISonarLintProject, List<BindingSuggestionDto>> suggestionsByProject = new HashMap<>();
    params.getSuggestions().forEach((configScopeId, suggestions) -> {
      var projectOpt = resolveProject(configScopeId);
      if (projectOpt.isPresent() && projectOpt.get().isOpen()) {
        suggestionsByProject.put(projectOpt.get(), suggestions);
      }
    });

    if (!suggestionsByProject.isEmpty()) {
      var firstProjectSuggestions = suggestionsByProject.values().iterator().next();
      if (suggestionsByProject.values().stream().allMatch(s -> areAllSameSuggestion(s, firstProjectSuggestions))) {
        // Suggest binding all projects to the suggestion
        Display.getDefault().asyncExec(() -> {
          var popup = new SingleBindingSuggestionPopup(List.copyOf(suggestionsByProject.keySet()), firstProjectSuggestions.get(0));
          popup.open();
        });
      } else {
        // Suggest binding all projects but let the user choose
        Display.getDefault().asyncExec(() -> {
          var popup = new BindingSuggestionPopup(List.copyOf(suggestionsByProject.keySet()));
          popup.open();
        });
      }
    }
  }

  private static boolean areAllSameSuggestion(List<BindingSuggestionDto> otherSuggestions, List<BindingSuggestionDto> firstProjectSuggestions) {
    if (otherSuggestions.size() != 1 || firstProjectSuggestions.size() != 1) {
      return false;
    }
    var suggestionDto = otherSuggestions.get(0);
    var firstSuggestion = firstProjectSuggestions.get(0);
    return Objects.equals(suggestionDto.getConnectionId(), firstSuggestion.getConnectionId())
      && Objects.equals(suggestionDto.getSonarProjectKey(), firstSuggestion.getSonarProjectKey());
  }

}
