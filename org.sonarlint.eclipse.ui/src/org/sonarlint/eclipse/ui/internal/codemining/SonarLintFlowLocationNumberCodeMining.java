/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

public class SonarLintFlowLocationNumberCodeMining extends AbstractCodeMining {

  private final int number;

  public SonarLintFlowLocationNumberCodeMining(MarkerFlowLocation location, Position position, SonarLintCodeMiningProvider provider, int number) {
    super(new Position(position.getOffset(), position.getLength()), provider, e -> onClick(e, location));
    this.number = number;
    setLabel(Integer.toString(number));
  }

  @Override
  public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
    String numberStr = Integer.toString(number);
    Point numberExtent = gc.stringExtent(numberStr);
    Point rect = new Point(numberExtent.x + 6, numberExtent.y);
    gc.setLineWidth(1);
    Color bgColor = new Color(gc.getDevice(), new RGB(209, 133, 130));
    Color fgColor = new Color(gc.getDevice(), new RGB(255, 255, 255));
    gc.setBackground(bgColor);
    gc.setForeground(fgColor);
    gc.fillRoundRectangle(x, y, rect.x - 2, rect.y, 5, 5);
    gc.drawString(numberStr, x + 2, y, true);
    bgColor.dispose();
    fgColor.dispose();
    return rect;
  }

  private static void onClick(MouseEvent e, MarkerFlowLocation location) {
    try {
      IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID);
      view.selectLocation(location);
    } catch (Exception ex) {
      SonarLintLogger.get().error("Unable to open Issue Location View", ex);
    }
  }

}
