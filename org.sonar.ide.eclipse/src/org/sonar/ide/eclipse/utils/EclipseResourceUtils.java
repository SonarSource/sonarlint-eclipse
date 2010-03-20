package org.sonar.ide.eclipse.utils;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.shared.AbstractResourceUtils;

/**
 * @author Jérémie Lagarde
 * 
 */
public class EclipseResourceUtils extends AbstractResourceUtils<IResource> {

  private static EclipseResourceUtils instance;

  public static EclipseResourceUtils getInstance() {
    if (instance == null) {
      instance = new EclipseResourceUtils();
    }
    return instance;
  }

  private EclipseResourceUtils() {
  }

  @Override
  protected boolean isJavaFile(IResource file) {
    return (JavaCore.create(file) != null);
  }

  @Override
  public String getFileName(IResource file) {
    return isJavaFile(file) ? StringUtils.substringBeforeLast(file.getName(), ".") : file.getName();
  }

  @Override
  protected String getDirectoryPath(IResource file) {
    throw new NotImplementedException("Currently only java files supported");
  }

  @Override
  protected String getPackageName(IResource file) {
    try {
      if (isJavaFile(file)) {
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(file);
        IPackageDeclaration[] packages = compilationUnit.getPackageDeclarations();
        StringBuilder name = null;
        for (IPackageDeclaration packageDeclaration : packages) {
          if (name == null) {
            name = new StringBuilder(packageDeclaration.getElementName());
          } else {
            name.append(".").append(packageDeclaration.getElementName());
          }
        }
        if(name == null)
          return "";
        return name.toString();
      }
    } catch (JavaModelException e) {
      // TODO Add exception management.
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getProjectKey(IResource file) {
    ProjectProperties properties = ProjectProperties.getInstance(file);
    return getProjectKey(properties.getGroupId(), properties.getArtifactId());
  }
}
