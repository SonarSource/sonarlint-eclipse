/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.preferences;

import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class SonarLintPreferences extends PropertyPage {
  public static final String NAME = "SonarLint";

  public SonarLintPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, NAME);
  }

  public void setTestFileRegularExpressions(String regex) {
    new DefaultText(this, 1).setText(regex);
  }

  public void setMarkersSeverity(MarkerSeverity severity) {
    new DefaultCombo(this, new WithLabelMatcher("SonarLint markers severity:")).setSelection(severity.getTextInCombo());
  }
  
  public void setIssueFilterPreference(IssueFilter filter) {
    new DefaultCombo(this, new WithLabelMatcher("SonarLint markers shown for:")).setSelection(filter.getTextInCombo());
  }

  public void setNewCodePreference(IssuePeriod period) {
    new DefaultCombo(this, new WithLabelMatcher("SonarLint markers shown on:")).setSelection(period.getTextInCombo());
  }

  public enum MarkerSeverity {
    ERROR("Error"), WARNING("Warning"), INFO("Info");

    private final String textInCombo;

    private MarkerSeverity(String textInCombo) {
      this.textInCombo = textInCombo;
    }

    public String getTextInCombo() {
      return textInCombo;
    }
  }

  public enum IssuePeriod {
    ALL_TIME("Overall code"),
    NEW_CODE("New code");

    private final String textInCombo;

    private IssuePeriod(String textInCombo) {
      this.textInCombo = textInCombo;
    }

    public String getTextInCombo() {
      return textInCombo;
    }
  }
  
  public enum IssueFilter {
    NON_RESOLVED("Non-resolved issues"),
    ALL_ISSUES("All issues (including resolved)");

    private final String textInCombo;

    private IssueFilter(String textInCombo) {
      this.textInCombo = textInCombo;
    }

    public String getTextInCombo() {
      return textInCombo;
    }
  }
}
