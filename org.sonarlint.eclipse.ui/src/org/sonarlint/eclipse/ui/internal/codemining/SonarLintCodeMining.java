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
package org.sonarlint.eclipse.ui.internal.codemining;

import java.util.function.Consumer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils.ExtraPosition;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

public class SonarLintCodeMining extends /* LineHeaderCodeMining */AbstractCodeMining {

  private final ExtraPosition position;

  public SonarLintCodeMining(IDocument doc, ExtraPosition position, SonarLintCodeMiningProvider provider) throws BadLocationException {
    /* super(doc.getLineOfOffset(position.getOffset()), doc, provider); */
    super(position, provider, null);
    this.position = position;
    setLabel(Integer.toString(position.getNumber()));
  }

  @Override
  public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
    String numberStr = Integer.toString(position.getNumber());
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

  @Override
  public Consumer<MouseEvent> getAction() {
    return e -> {
      try {
        IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID);
        view.selectPosition(position.getNumber());
      } catch (Exception ex) {
        SonarLintLogger.get().error("Unable to open Issue Location View", ex);
      }
    };
  }

}
