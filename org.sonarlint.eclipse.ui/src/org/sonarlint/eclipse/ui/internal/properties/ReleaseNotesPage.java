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
package org.sonarlint.eclipse.ui.internal.properties;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.SonarLintWebView;

/**
 *  New preference page displaying the latest Release Notes of SonarLint. We cannot display all due to the preference
 *  page not being able to display content that is scrollable (e.g. ScrollableComposite).
 */
public class ReleaseNotesPage extends PropertyPage implements IWorkbenchPreferencePage {
  public static final String ABOUT_CONFIGURATION_ID = "org.sonarlint.eclipse.ui.properties.ReleaseNotesPage";
  private static final String RELEASE_NOTES_HTML = "/intro/RELEASE_NOTES.html";

  public ReleaseNotesPage() {
    setTitle("Release Notes");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Here you can find the latest Release Notes. This won't include the Release Notes of all versions, "
      + "including ones released after the installed one!");
  }

  @Override
  protected Control createContents(final Composite parent) {
    var composite = new Composite(parent, SWT.NONE);
    var gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    var browser = new SonarLintWebView(composite, false);
    browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    browser.setHtmlBody(loadBundledReleaseNotes());

    return composite;
  }

  /**
   *  We read the Release Notes from the HTML document from within the bundle (org.sonarlint.eclipse.ui) and then
   *  display it in our SonarLintWebView (a browser). In case the HTML could not be read, there are different reasons
   *  for why this is not possible, we display a fallback message linking the user to the Sonar Community forum and the
   *  GitHub Release page.
   *
   *  @return
   */
  private static String loadBundledReleaseNotes() {
    var message = "<h1>The locally stored Release Notes could not be loaded!</h1>"
      + "<p>"
      + "  Please take a look at either the <a href=\"" + SonarLintDocumentation.COMMUNITY_FORUM_ECLIPSE_RELEASES
      + "\">Sonar Community Forum</a> or the <a href=\"" + SonarLintDocumentation.GITHUB_RELEASES
      + "\">GitHub releases page</a> for Release Notes for all (including newer) versions."
      + "</p>";

    try {
      var bundle = SonarLintUiPlugin.getDefault().getBundle();
      var fileUrl = bundle.getEntry(RELEASE_NOTES_HTML);
      var file = new File(FileLocator.toFileURL(fileUrl).getFile());

      try (var is = new FileInputStream(file)) {
        message = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception err) {
      SonarLintLogger.get().error("Cannot read '" + RELEASE_NOTES_HTML + "', located inside plug-in!", err);
    }

    return message;
  }
}
