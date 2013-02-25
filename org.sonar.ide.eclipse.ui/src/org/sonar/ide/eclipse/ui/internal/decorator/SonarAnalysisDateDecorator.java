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
