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
package org.sonarlint.eclipse.ui.internal.util;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.telemetry.LinkTelemetry;

public final class BrowserUtils {
  private BrowserUtils() {
    // utility class
  }

  public static void openExternalBrowser(String url, Display display) {
    // For unit tests we want to disable the actual browser opening
    var externalBrowserDisabled = System.getProperty("sonarlint.internal.externalBrowser.disabled");
    if (externalBrowserDisabled == null) {
      display.asyncExec(() -> {
        try {
          PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (PartInitException | MalformedURLException e) {
          SonarLintLogger.get().error("Unable to open external browser", e);
        }
      });
    }
  }

  /**
   * In scope of MMF-3451, we want to log the number of clicks on links related to the topic of
   * "connected mode setup UI helps users discover the value of Sonar solution and connected mode"
   */
  public static void openExternalBrowserWithTelemetry(LinkTelemetry link, Display display) {
    SonarLintCorePlugin.getTelemetry().helpAndFeedbackLinkClicked(link.getLinkId());
    openExternalBrowser(link.getUrl(), display);
  }

  public static void addLinkListener(Browser browser) {
    browser.addLocationListener(new LocationAdapter() {
      @Override
      public void changing(LocationEvent event) {
        var loc = event.location;

        if ("about:blank".equals(loc)) { //$NON-NLS-1$
          /*
           * Using the Browser.setText API triggers a location change to "about:blank".
           * XXX: remove this code once https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is fixed
           */
          // input set with setText
          return;
        }

        event.doit = false;

        openExternalBrowser(loc, browser.getDisplay());
      }
    });
  }
}
