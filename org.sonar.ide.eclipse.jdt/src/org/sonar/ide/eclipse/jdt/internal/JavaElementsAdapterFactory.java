package org.sonar.ide.eclipse.jdt.internal;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.*;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.wsclient.services.Resource;

/**
 * Adapter factory for Java elements.
 */
@SuppressWarnings("rawtypes")
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static final char DELIMITER = ':';

  private static final char PACKAGE_DELIMITER = '.';

  /**
   * Default package name for classes without package definition.
   */
  private static final String DEFAULT_PACKAGE_NAME = "[default]";

  private static Class<?>[] ADAPTER_LIST = { ISonarResource.class, Resource.class, IFile.class };

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarResource.class.equals(adapterType)) {
      return getSonarResource(adaptableObject);
    } else if (adapterType == Resource.class) {
      if (adaptableObject instanceof IJavaProject) {
        IJavaProject javaProject = (IJavaProject) adaptableObject;
        String key = getProjectKey(javaProject.getProject());
        return new Resource().setKey(key);
      }
    } else if (adapterType == IFile.class) {
      if (adaptableObject instanceof Resource) {
        Resource resource = (Resource) adaptableObject;
        String key = resource.getKey();
        String[] parts = StringUtils.split(key, DELIMITER);
        String groupId = parts[0];
        String artifactId = parts[1];
        String className = parts[2];

        IWorkspace root = ResourcesPlugin.getWorkspace();
        // TODO this is not optimal
        for (IProject project : root.getRoot().getProjects()) {
          if (project.isAccessible()) {
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
        }
        return null;
      }
    }
    return null;
  }

  private Object getSonarResource(Object adaptableObject) {
    if (adaptableObject instanceof IJavaElement) {
      IJavaElement javaElement = (IJavaElement) adaptableObject;
      return getAdapter(javaElement.getResource(), ISonarResource.class);
    } else if (adaptableObject instanceof IProject) {
      IProject project = (IProject) adaptableObject;
      if ( !isOpenAndHasSonarNature(project)) {
        return null;
      }
      return SonarPlugin.createSonarResource(project, getProjectKey(project));
    } else if (adaptableObject instanceof IFolder) {
      IFolder folder = (IFolder) adaptableObject;
      IProject project = folder.getProject();
      if ( !isOpenAndHasSonarNature(project)) {
        return null;
      }
      String projectKey = getProjectKey(folder.getProject());
      String packageKey = getPackageKey(JavaCore.create(folder));
      if (packageKey != null) {
        return SonarPlugin.createSonarResource(folder, projectKey + DELIMITER + packageKey);
      }
    } else if (adaptableObject instanceof IFile) {
      IFile file = (IFile) adaptableObject;
      IProject project = file.getProject();
      if ( !isOpenAndHasSonarNature(project)) {
        return null;
      }
      String projectKey = getProjectKey(file.getProject());
      IJavaElement javaElement = JavaCore.create(file);
      if (javaElement instanceof ICompilationUnit) {
        String packageKey = getPackageKey(javaElement.getParent());
        String classKey = StringUtils.substringBeforeLast(javaElement.getElementName(), ".");
        return SonarPlugin.createSonarResource(file, projectKey + DELIMITER + packageKey + PACKAGE_DELIMITER + classKey);
      }
    }
    return null;
  }

  private boolean isOpenAndHasSonarNature(IProject project) {
    return project.isAccessible() && SonarPlugin.hasSonarNature(project);
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

  private String getPackageKey(IJavaElement javaElement) {
    String packageName = null;
    if (javaElement instanceof IPackageFragmentRoot) {
      packageName = DEFAULT_PACKAGE_NAME;
    } else if (javaElement instanceof IPackageFragment) {
      IPackageFragment packageFragment = (IPackageFragment) javaElement;
      packageName = packageFragment.getElementName();
      if ("".equals(packageName)) {
        return DEFAULT_PACKAGE_NAME;
      }
    }
    return packageName;
  }

  private String getProjectKey(IProject project) {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    return getProjectKey(properties.getGroupId(), properties.getArtifactId(), properties.getBranch());
  }

  private String getProjectKey(String groupId, String artifactId, String branch) {
    if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId)) {
      return null;
    }
    StringBuilder sb = new StringBuilder().append(groupId).append(DELIMITER).append(artifactId);
    if (StringUtils.isNotBlank(branch)) {
      sb.append(DELIMITER).append(branch);
    }
    return sb.toString();
  }
}
