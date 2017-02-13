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
package org.sonarlint.eclipse.ui.internal.views;

import java.lang.reflect.Method;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionWebView extends AbstractLinkedSonarWebView<IMarker> implements ISelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.RuleDescriptionWebView";

  private String css() {
    return "<style type=\"text/css\">"
      + "body { font-family: Helvetica Neue,Segoe UI,Helvetica,Arial,sans-serif; font-size: 13px; line-height: 1.23076923; "
      + "color: " + hexColor(getBrowser().getForeground()) + ";background-color: " + hexColor(getBrowser().getBackground())
      + "}"
      + "h1 { color: " + hexColor(getBrowser().getForeground()) + ";font-size: 14px;font-weight: 500; }"
      + "h2 { line-height: 24px; color: " + hexColor(getBrowser().getForeground()) + ";}"
      + "a { border-bottom: 1px solid #cae3f2; color: #236a97; cursor: pointer; outline: none; text-decoration: none; transition: all .2s ease;}"
      + ".rule-desc { line-height: 1.5;}"
      + ".rule-desc { line-height: 1.5;}"
      + ".rule-desc h2 { font-size: 16px; font-weight: 400;}"
      + ".rule-desc code { padding: .2em .45em; margin: 0; background-color: " + hexColor(getBrowser().getForeground(), 20) + "; border-radius: 3px; white-space: nowrap;}"
      + ".rule-desc pre { padding: 10px; border-top: 1px solid " + hexColor(getBrowser().getForeground(), 100) + "; border-bottom: 1px solid "
      + hexColor(getBrowser().getForeground(), 100)
      + "; line-height: 18px; overflow: auto;}"
      + ".rule-desc code, .rule-desc pre { font-family: Consolas,Liberation Mono,Menlo,Courier,monospace; font-size: 12px;}"
      + ".rule-desc ul { padding-left: 40px; list-style: disc;}</style>";
  }

  @Override
  protected void open(IMarker element) {
    try {
      String ruleName = element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR).toString();
      String ruleKey = element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR).toString();
      SonarLintProject p = SonarLintProject.getInstance(element.getResource());
      String htmlDescription;
      if (StringUtils.isBlank(p.getServerId())) {
        htmlDescription = SonarLintCorePlugin.getDefault().getDefaultSonarLintClientFacade().getHtmlRuleDescription(ruleKey);
      } else {
        IServer server = ServersManager.getInstance().getServer(p.getServerId());
        if (server == null) {
          super.showMessage("Project " + p.getProject().getName() + " is linked to an unknown server: " + p.getServerId() + ". Please update configuration.");
          return;
        }
        htmlDescription = server.getHtmlRuleDescription(ruleKey);
      }

      super.showHtml("<!doctype html><html><head>" + css() + "</head><body><h1><big>"
        + ruleName + "</big> (" + ruleKey
        + ")</h1><div class=\"rule-desc\">" + htmlDescription
        + "</div></body></html>");
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to open rule description", e);
    }
  }

  private static String hexColor(Color color) {
    return hexColor(color, getAlpha(color));
  }

  private static int getAlpha(Color c) {
    try {
      Method m = Color.class.getMethod("getAlpha");
      return (int) m.invoke(c);
    } catch (Exception e) {
      return 255;
    }
  }

  private static String hexColor(Color color, Integer alpha) {
    return "rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + alpha / 255.0 + ")";
  }

  @Override
  protected IMarker findSelectedElement(IWorkbenchPart part, ISelection selection) {
    return findSelectedSonarIssue(selection);
  }

}
