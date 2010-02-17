package org.sonar.ide.eclipse.views.model;

import org.sonar.wsclient.services.Resource;

/**
 * @author Jérémie Lagarde
 * 
 */
public class TreeFile extends TreeObject {

	public TreeFile(Resource resource) {
		super(resource);
	}

	@Override
	public String getRemoteURL() {
		return getRemoteRootURL() + "/" + "resource/index/" + getResource().getId(); //$NON-NLS-1$
	}

}
