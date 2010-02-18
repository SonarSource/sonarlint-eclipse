package org.sonar.ide.eclipse.views.model;

import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

/**
 * @author Jérémie Lagarde
 */
public class TreeProject extends TreeParent {

  public TreeProject(Resource resource) {
    super(resource);
  }


  @Override
  public String getName() {
    return super.getName() + " - " + getResource().getVersion();
  }

  @Override
  protected ResourceQuery createResourceQuery() {
    ResourceQuery query = new ResourceQuery();
    query.setResourceKeyOrId(getResource().getKey());
    query.setDepth(1);
    return query;
  }

  @Override
  public String getRemoteURL() {
    return getRemoteRootURL() + "/" + "project/index/" + getResource().getId(); //$NON-NLS-1$
  }
}
