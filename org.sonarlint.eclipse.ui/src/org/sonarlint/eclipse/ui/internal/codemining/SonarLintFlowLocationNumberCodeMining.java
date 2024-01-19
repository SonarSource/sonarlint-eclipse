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
package org.sonarlint.eclipse.ui.internal.codemining;

import java.util.Optional;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.ui.internal.ColorUtil;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

public class SonarLintFlowLocationNumberCodeMining extends AbstractCodeMining {

  private static final double HORIZONTAL_MARGIN_RATIO = 0.2;
  private static final double HORIZONTAL_PADDING_RATIO = 0.2;
  private static final double ARC_RADIUS_RATIO = 0.5;

  private static final RGB LIGHT_BACKGROUND = new RGB(0xd1, 0x85, 0x82);
  private static final RGB LIGHT_SELECTED_BACKGROUND = new RGB(0xa4, 0x03, 0x0f);

  private static final RGB DARK_BACKGROUND = new RGB(0x74, 0x23, 0x2f);
  private static final RGB DARK_SELECTED_BACKGROUND = new RGB(0xb4, 0x13, 0x1f);

  private static final RGB LIGHT_FOREGROUND = new RGB(0xff, 0xff, 0xff);
  private static final RGB LIGHT_SELECTED_FOREGROUND = new RGB(0xff, 0xff, 0xff);

  private static final RGB DARK_FOREGROUND = new RGB(0xc0, 0xc0, 0xc0);
  private static final RGB DARK_SELECTED_FOREGROUND = new RGB(0xff, 0xff, 0xff);

  private final int number;
  private final boolean isSelected;

  public SonarLintFlowLocationNumberCodeMining(MarkerFlowLocation location, Position position, SonarLintCodeMiningProvider provider, int number, boolean isSelected) {
    super(new Position(position.getOffset(), position.getLength()), provider, e -> onClick(location));
    this.number = number;
    this.isSelected = isSelected;
    setLabel(Integer.toString(number));
  }

  @Override
  public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
    var isDark = ColorUtil.isDark(textWidget.getShell().getBackground().getRGB());
    gc.setAntialias(SWT.ON);
    var numberStr = Integer.toString(number);
    var numberExtent = gc.stringExtent(numberStr);
    // Compute all sizes based on text size to adapt to zoom in/out
    var arcRadius = (int) (numberExtent.y * ARC_RADIUS_RATIO);
    var horizontalPadding = (int) (numberExtent.y * HORIZONTAL_PADDING_RATIO);
    var horizontalMargin = (int) (numberExtent.y * HORIZONTAL_MARGIN_RATIO);
    var labelRect = new Point(numberExtent.x + 2 * (horizontalMargin + horizontalPadding), numberExtent.y);
    gc.setLineWidth(1);
    var bgColor = new Color(gc.getDevice(), getBackgroundRGB(isSelected, isDark));
    var fgColor = new Color(gc.getDevice(), getForegroundRGB(isSelected, isDark));
    gc.setBackground(bgColor);
    gc.setForeground(fgColor);
    gc.fillRoundRectangle(x + horizontalMargin, y, labelRect.x - 2 * horizontalMargin, labelRect.y, arcRadius, arcRadius);
    gc.drawString(numberStr, x + horizontalMargin + horizontalPadding, y, true);
    bgColor.dispose();
    fgColor.dispose();
    return labelRect;
  }

  private static RGB getBackgroundRGB(boolean selected, boolean isDark) {
    return selected ? getSelectedBackgroundRGB(isDark) : getUnselectedBackgroundRGB(isDark);
  }

  private static RGB getSelectedBackgroundRGB(boolean isDark) {
    return isDark ? DARK_SELECTED_BACKGROUND : LIGHT_SELECTED_BACKGROUND;
  }

  private static RGB getUnselectedBackgroundRGB(boolean isDark) {
    return isDark ? DARK_BACKGROUND : LIGHT_BACKGROUND;
  }

  private static RGB getForegroundRGB(boolean selected, boolean isDark) {
    return selected ? getSelectedForegroundRGB(isDark) : getUnselectedForegroundRGB(isDark);
  }

  private static RGB getSelectedForegroundRGB(boolean isDark) {
    return isDark ? DARK_SELECTED_FOREGROUND : LIGHT_SELECTED_FOREGROUND;
  }

  private static RGB getUnselectedForegroundRGB(boolean isDark) {
    return isDark ? DARK_FOREGROUND : LIGHT_FOREGROUND;
  }

  private static void onClick(MarkerFlowLocation location) {
    findLocationsView().ifPresent(view -> view.selectLocation(location));
  }

  private static Optional<IssueLocationsView> findLocationsView() {
    try {
      return Optional.of((IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID));
    } catch (Exception ex) {
      SonarLintLogger.get().error("Unable to open Issue Location View", ex);
      return Optional.empty();
    }
  }
}
