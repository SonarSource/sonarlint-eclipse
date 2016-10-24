package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Collection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class MarkerUpdater implements TrackingChangeListener {

  @Override
  public void onTrackingChange(String moduleKey, String file, Collection<? extends Trackable> issues) {
    // TODO find the absolute path from moduleKey and file (relative path)
    String absolutePath = file;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IPath location = Path.fromOSString(absolutePath);
    IFile ifile = workspace.getRoot().getFileForLocation(location);

    for (IMarker marker : MarkerUtils.findMarkers(ifile)) {
      System.out.println(marker);
    }
    System.out.println("done");

    // TODO delete all markers, create new
    // See MarkerUtils.deleteIssuesMarkers
  }

}
