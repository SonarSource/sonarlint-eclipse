/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Locale;
import javax.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionPart {

  private Browser browser;
  private String extraCss = "";

  public RuleDescriptionPart(Browser browser) {
    this.browser = browser;
  }

  private String css() {
    return "<style type=\"text/css\">"
      + "body { font-family: Helvetica Neue,Segoe UI,Helvetica,Arial,sans-serif; font-size: 13px; line-height: 1.23076923; "
      + "color: " + hexColor(browser.getForeground()) + ";background-color: " + hexColor(browser.getBackground())
      + "}"
      + "h1 { color: " + hexColor(browser.getForeground()) + ";font-size: 14px;font-weight: 500; }"
      + "h2 { line-height: 24px; color: " + hexColor(browser.getForeground()) + ";}"
      + "a { border-bottom: 1px solid #cae3f2; color: #236a97; cursor: pointer; outline: none; text-decoration: none; transition: all .2s ease;}"
      + ".rule-desc { line-height: 1.5;}"
      + ".rule-desc { line-height: 1.5;}"
      + ".rule-desc h2 { font-size: 16px; font-weight: 400;}"
      + ".rule-desc code { padding: .2em .45em; margin: 0; background-color: " + hexColor(browser.getForeground(), 20) + "; border-radius: 3px; white-space: nowrap;}"
      + ".rule-desc pre { padding: 10px; border-top: 1px solid " + hexColor(browser.getForeground(), 100) + "; border-bottom: 1px solid "
      + hexColor(browser.getForeground(), 100)
      + "; line-height: 18px; overflow: auto;}"
      + ".rule-desc code, .rule-desc pre { font-family: Consolas,Liberation Mono,Menlo,Courier,monospace; font-size: 12px;}"
      + ".rule-desc ul { padding-left: 40px; list-style: disc;}"
      + extraCss
      + "</style>";
  }

  public void updateView(RuleDetails ruleDetails) {
    String ruleName = ruleDetails.getName();
    String ruleKey = ruleDetails.getKey();
    String htmlDescription = ruleDetails.getHtmlDescription();
    String extendedDescription = ruleDetails.getExtendedDescription();
    if (!extendedDescription.isEmpty()) {
      htmlDescription += "<div>" + extendedDescription + "</div>";
    }
    String type = ruleDetails.getType();
    String typeImg64 = getAsBase64(SonarLintImages.getTypeImage(type));
    String severity = ruleDetails.getSeverity();
    String severityImg64 = getAsBase64(SonarLintImages.getSeverityImage(severity));
    browser.setText("<!doctype html><html><head>" + css() + "</head><body><h1><big>"
      + ruleName + "</big> (" + ruleKey + ")</h1>"
      + "<div>"
      + "<img style=\"padding-bottom: 1px;vertical-align: middle\" width=\"16\" height=\"16\" alt=\"" + type + "\" src=\"data:image/gif;base64," + typeImg64 + "\">&nbsp;"
      + clean(type)
      + "&nbsp;"
      + "<img style=\"padding-bottom: 1px;vertical-align: middle\" width=\"16\" height=\"16\" alt=\"" + severity + "\" src=\"data:image/gif;base64," + severityImg64 + "\">&nbsp;"
      + clean(severity)
      + "</div>"
      + "<div class=\"rule-desc\">" + htmlDescription
      + "</div></body></html>");
  }

  private static String clean(String txt) {
    return StringUtils.capitalize(txt.toLowerCase(Locale.ENGLISH).replace("_", " "));
  }

  private static String getAsBase64(@Nullable Image image) {
    if (image == null) {
      return "";
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageLoader loader = new ImageLoader();
    loader.data = new ImageData[] {image.getImageData()};
    loader.save(out, SWT.IMAGE_PNG);
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }

  private static String hexColor(Color color, Integer alpha) {
    return "rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + alpha / 255.0 + ")";
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

  public void setExtraCss(String extraCss) {
    this.extraCss = extraCss;
  }
}
