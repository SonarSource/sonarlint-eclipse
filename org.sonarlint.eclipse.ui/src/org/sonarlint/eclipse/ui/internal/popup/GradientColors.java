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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;

/**
 * Based on FormColors of UI Forms.
 * 
 * @author Benjamin Pasero (initial contribution from RSSOwl, see bug 177974)
 * @author Mik Kersten
 * @since 3.7
 */
public class GradientColors {

  private static final String ACTIVE_TAB_BG_END = "org.eclipse.ui.workbench.ACTIVE_TAB_BG_END";

  private static final String ACTIVE_TAB_BG_START = "org.eclipse.ui.workbench.ACTIVE_TAB_BG_START";

  private static final String ACTIVE_TAB_OUTER_KEYLINE_COLOR = "org.eclipse.ui.workbench.ACTIVE_TAB_OUTER_KEYLINE_COLOR";

  private static final String ACTIVE_TAB_SELECTED_TEXT_COLOR = "org.eclipse.ui.workbench.ACTIVE_TAB_SELECTED_TEXT_COLOR";

  private Color titleText;

  private Color gradientBegin;

  private Color gradientEnd;

  private Color border;

  private final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

  public GradientColors() {
    createColors();
  }

  private void createColors() {
    createBorderColor();
    createGradientColors();
    titleText = colorRegistry.get(ACTIVE_TAB_SELECTED_TEXT_COLOR);
  }

  public Color getGradientBegin() {
    return gradientBegin;
  }

  public Color getGradientEnd() {
    return gradientEnd;
  }

  public Color getBorder() {
    return border;
  }

  public Color getTitleText() {
    return titleText;
  }

  private void createBorderColor() {
    border = colorRegistry.get(ACTIVE_TAB_OUTER_KEYLINE_COLOR);
  }

  private void createGradientColors() {
    gradientBegin = colorRegistry.get(ACTIVE_TAB_BG_START);
    gradientEnd = colorRegistry.get(ACTIVE_TAB_BG_END);
  }
}
