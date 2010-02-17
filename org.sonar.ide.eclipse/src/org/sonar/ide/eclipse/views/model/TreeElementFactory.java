package org.sonar.ide.eclipse.views.model;

import org.sonar.wsclient.services.Resource;

/**
 * @author Jérémie Lagarde
 * 
 */
public final class TreeElementFactory {

	public enum SonarResourceScope {
		PRJ, // project/module
		DIR, // directory (like Java package)
		FIL  // file
	}

	public enum SonarResourceQualifier {
		TRK, // project
		BRC, // module
		CLA, // class
		UTS, // unit test
		DIR, // directory
		FIL
		// file
	}

	private TreeElementFactory() {
	}

	public static TreeObject create(Resource resource) {
		if (resource == null)
			return null;
		switch (SonarResourceScope.valueOf(resource.getScope())) {
			case PRJ: return new TreeProject(resource);
			case DIR: return new TreeDirectory(resource);
			case FIL: return new TreeFile(resource);
		default:
			break;
		}
		return null;
	}

}
