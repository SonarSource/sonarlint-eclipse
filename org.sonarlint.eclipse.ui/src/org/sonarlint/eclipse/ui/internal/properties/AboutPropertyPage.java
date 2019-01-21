/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class AboutPropertyPage extends PropertyPage implements IWorkbenchPreferencePage {

  private Button enabledBtn;

  public AboutPropertyPage() {
    setTitle("Miscellaneous");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Statistics");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(final Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    Link text = new Link(composite, SWT.NONE);
    GridData textGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
    text.setLayoutData(textGd);
    text.setText("By sharing anonymous SonarLint usage statistics, you help us understand how SonarLint is used so "
      + "we can improve the plugin to work even better for you.\nWe don't collect source code, IP addresses, or any personally identifying "
      + "information. And we don't share the data with anyone else.\n\nSee a <a href=\"#\">sample of the data.</a>");

    final DefaultToolTip tip = new DefaultToolTip(text, ToolTip.RECREATE, true);
    tip.setText("{\n"
      + "    \"days_since_installation\": 27,\n"
      + "    \"days_of_use\": 5,\n"
      + "    \"sonarlint_version\": \"3.3.1\",\n"
      + "    \"sonarlint_product\": \"SonarLint Eclipse\",\n"
      + "    \"connected_mode_used\": true\n"
      + "    \"connected_mode_sonarcloud\": true\n"
      + "    \"system_time\":\"2018-02-27T16:31:49.173+01:00\",\n"
      + "    \"install_time\":\"2018-02-01T16:30:49.124+01:00\"\n"
      + "    \"analyses\":[{\"language\":\"java\",\"rate_per_duration\":{\"0-300\":100,\"300-500\":0,\"500-1000\":0,\"1000-2000\":0,\"2000-4000\":0,\"4000+\":0}}]\n"
      + "}");

    text.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Point cursorLocation = PlatformUI.getWorkbench().getDisplay().getCursorLocation();
        tip.show(PlatformUI.getWorkbench().getDisplay().map(null, text, cursorLocation.x, cursorLocation.y));
      }
    });

    enabledBtn = new Button(composite, SWT.CHECK);
    enabledBtn.setText("Share anonymous SonarLint statistics");
    enabledBtn.setSelection(SonarLintCorePlugin.getTelemetry().enabled());
    GridData layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    enabledBtn.setLayoutData(layoutData);

    return composite;
  }

  @Override
  public boolean performOk() {
    SonarLintCorePlugin.getTelemetry().optOut(!enabledBtn.getSelection());
    return true;
  }

  @Override
  protected void performDefaults() {
    enabledBtn.setSelection(true);
  }

}
