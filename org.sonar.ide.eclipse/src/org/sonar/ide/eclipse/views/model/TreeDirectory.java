package org.sonar.ide.eclipse.views.model;

import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

/**
 * @author Jérémie Lagarde
 * 
 */
public class TreeDirectory extends TreeParent {

	public TreeDirectory(Resource resource) {
		super(resource);
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
		return getRemoteRootURL() + "/" + "resource/index/" + getResource().getId(); //$NON-NLS-1$
	}

}
