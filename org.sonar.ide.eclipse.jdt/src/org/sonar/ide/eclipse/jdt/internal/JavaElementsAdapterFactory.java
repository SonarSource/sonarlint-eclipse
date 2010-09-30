package org.sonar.ide.eclipse.jdt.internal;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.wsclient.services.Resource;

/**
 * Adapter factory for Java elements.
 */
@SuppressWarnings("unchecked")
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static Class[] ADAPTER_LIST = { Resource.class, IFile.class };

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adapterType == Resource.class) {
      if (adaptableObject instanceof IJavaProject) {
        IJavaProject javaProject = (IJavaProject) adaptableObject;
        String key = EclipseResourceUtils.getInstance().getProjectKey(javaProject.getResource());
        return new Resource().setKey(key);
      }
    } else if (adapterType == IFile.class) {
      if (adaptableObject instanceof Resource) {
        Resource resource = (Resource) adaptableObject;
        String key = resource.getKey();
        String[] parts = StringUtils.split(key, ":");
        String groupId = parts[0];
        String artifactId = parts[1];
        String className = parts[2];

        IWorkspace root = ResourcesPlugin.getWorkspace();
        for (IProject project : root.getRoot().getProjects()) {
          ProjectProperties props = ProjectProperties.getInstance(project);
          if (StringUtils.equals(props.getGroupId(), groupId) && StringUtils.equals(props.getArtifactId(), artifactId)) {
            IJavaProject javaProject = JavaCore.create(project);
            try {
              IType type = javaProject.findType(className);
              IResource result = type.getCompilationUnit().getResource();
              return result instanceof IFile ? result : null;
            } catch (JavaModelException e) {
              SonarLogger.log(e);
            }
          }
        }
        return null;
      }
    }
    return null;
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
