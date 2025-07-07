/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.preferences;

import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class SonarLintPreferences extends PropertyPage {
  public static final String NAME = "SonarQube";

  public SonarLintPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, NAME);
  }

  public void setTestFileRegularExpressions(String regex) {
    new DefaultText(this, 1).setText(regex);
  }

  public String getNodeJsPath() {
    return new DefaultText(this, 2).getMessage();
  }

  public void setMarkersSeverity(MarkerSeverity severity) {
    new DefaultCombo(this, new WithLabelMatcher("SonarQube markers severity:")).setSelection(severity.getTextInCombo());
  }

  public void setShowAllMarkers(boolean showAllMarkers) {
    new CheckBox(this, new WithTextMatcher("Show SonarQube markers for open and resolved issues")).toggle(showAllMarkers);
  }

  public void setFocusOnNewCode(boolean focusOnNewCode) {
    new CheckBox(this, new WithTextMatcher("Show SonarQube markers only for new code")).toggle(focusOnNewCode);
  }

  public void enableSonarQubeCloudRegionEA(boolean enableEarlyAccess) {
    new CheckBox(this, new WithTextMatcher("Show region selection for SonarQube Cloud (Early Access)")).toggle(enableEarlyAccess);
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
}
