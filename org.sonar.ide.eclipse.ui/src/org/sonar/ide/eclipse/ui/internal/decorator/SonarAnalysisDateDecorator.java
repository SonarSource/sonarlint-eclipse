/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.decorator;

import org.apache.commons.lang.time.DateUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SonarAnalysisDateDecorator implements ILabelDecorator {

  private SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM/yyyy");
  private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

  public Image decorateImage(Image image, Object element) {
    return null;
  }

  public String decorateText(String text, Object element) {
    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      IProject project = resource.getProject();
      if (project != null) {
        SonarProject projectProperties = SonarProject.getInstance(project);
        if (projectProperties != null && projectProperties.getLastAnalysisDate() != null) {
          Date lastAnalysisDate = projectProperties.getLastAnalysisDate();
          if (DateUtils.isSameDay(lastAnalysisDate, Calendar.getInstance().getTime())) {
            return text + " " + sdfTime.format(lastAnalysisDate);
          }
          else {
            return text + " " + sdfDay.format(lastAnalysisDate);
          }
        }
      }
    }
    return null;
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void addListener(final ILabelProviderListener listener) {
  }

  public void removeListener(ILabelProviderListener listener) {
  }

  public void dispose() {
  }

}
