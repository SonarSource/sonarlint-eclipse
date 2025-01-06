/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.swt.graphics.RGB;

/**
 * Inspired by https://github.com/JetBrains/intellij-community/blob/f95571006c2b9a19d5c11c20d6120101d20342eb/platform/util/ui/src/com/intellij/ui/ColorUtil.java
 */
public final class ColorUtil {

  private ColorUtil() {
    // NOP
  }

  public static boolean isDark(RGB rgb) {
    return ((getLuminance(rgb) + 0.05) / 0.05) < 4.5;
  }

  /**
   * @see <a href="https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef">W3C relative luminance definition<a/>
   */
  public static double getLuminance(RGB rgb) {
    return getLinearRGBComponentValue(rgb.red / 255.0) * 0.2126 +
           getLinearRGBComponentValue(rgb.green / 255.0) * 0.7152 +
           getLinearRGBComponentValue(rgb.blue / 255.0) * 0.0722;
  }

  private static double getLinearRGBComponentValue(double colorValue) {
    if (colorValue <= 0.03928) {
      return colorValue / 12.92;
    }
    return Math.pow(((colorValue + 0.055) / 1.055), 2.4);
  }
}
