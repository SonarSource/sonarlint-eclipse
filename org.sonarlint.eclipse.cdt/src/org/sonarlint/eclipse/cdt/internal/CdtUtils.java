/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.cdt.internal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.internal.jobs.DefaultPreAnalysisContext;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class CdtUtils {
  private static final String CFAMILY_USE_CACHE = "sonar.cfamily.useCache";
  private static final String BUILD_WRAPPER_OUTPUT_PROP = "sonar.cfamily.build-wrapper-output";
  private static final String BUILD_WRAPPER_OUTPUT_FILENAME = "build-wrapper-dump.json";
  private static final Charset BUILD_WRAPPER_OUTPUT_CHARSET = StandardCharsets.UTF_8;
  private final BuildWrapperJsonFactory jsonFactory;
  private final CCorePlugin cCorePlugin;
  private final Predicate<IFile> fileValidator;
  private final SonarLintLogger logger;
  private final BiFunction<IProject, String, IContentType> contentTypeResolver;

  public CdtUtils() {
    this(new BuildWrapperJsonFactory(), CCorePlugin.getDefault(), CoreModel::isTranslationUnit,
      CCorePlugin::getContentType, SonarLintLogger.get());
  }

  public CdtUtils(BuildWrapperJsonFactory jsonFactory, CCorePlugin cCorePlugin, Predicate<IFile> fileValidator,
    BiFunction<IProject, String, IContentType> contentTypeResolver, SonarLintLogger logger) {
    this.jsonFactory = jsonFactory;
    this.cCorePlugin = cCorePlugin;
    this.fileValidator = fileValidator;
    this.logger = logger;
    this.contentTypeResolver = contentTypeResolver;
  }

  public boolean hasCOrCppNature(IProject project) {
    try {
      return project.hasNature(CProjectNature.C_NATURE_ID) || project.hasNature(CCProjectNature.CC_NATURE_ID);
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return false;
    }
  }

  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    var filesToAnalyze = context.getFilesToAnalyze()
      .stream()
      .filter(f -> f.getResource() instanceof IFile && fileValidator.test((IFile) f.getResource()))
      .collect(Collectors.toList());

    try {
      var configuredFiles = configureCProject(context, context.getProject(), filesToAnalyze);
      var jsonPath = writeJson(context, context.getProject(), configuredFiles);
      logger.debug("Wrote build info to: " + jsonPath.toString());
      context.setAnalysisProperty(CFAMILY_USE_CACHE, Boolean.FALSE.toString());
      context.setAnalysisProperty(BUILD_WRAPPER_OUTPUT_PROP, jsonPath.getParent().toString());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  private Collection<ConfiguredFile> configureCProject(IPreAnalysisContext context, ISonarLintProject project, Collection<ISonarLintFile> filesToAnalyze) {
    var files = new LinkedList<ConfiguredFile>();
    var infoProvider = cCorePlugin.getScannerInfoProvider((IProject) project.getResource());

    for (ISonarLintFile file : filesToAnalyze) {
      var builder = new ConfiguredFile.Builder((IFile) file.getResource());

      var path = ((DefaultPreAnalysisContext) context).getLocalPath(file);
      var fileInfo = infoProvider.getScannerInformation(file.getResource());

      builder.includes(fileInfo.getIncludePaths() != null ? fileInfo.getIncludePaths() : new String[0])
        .symbols(fileInfo.getDefinedSymbols() != null ? fileInfo.getDefinedSymbols() : Collections.emptyMap())
        .path(path);

      files.add(builder.build());
    }
    return files;

  }

  private Path writeJson(IPreAnalysisContext context, ISonarLintProject project, Collection<ConfiguredFile> files) throws IOException {
    var json = jsonFactory.create(files, getBaseDir(context, project));
    return createJsonFile(context.getAnalysisTemporaryFolder(), json);
  }

  private static String getBaseDir(IPreAnalysisContext context, ISonarLintProject project) {
    var projectLocation = project.getResource().getLocation();
    if (projectLocation != null) {
      return projectLocation.toFile().toString();
    }
    // In some unfrequent cases the project may be virtual and don't have physical location
    // so fallback to use analysis work dir (where physical file copy should be created anyway)
    return context.getAnalysisTemporaryFolder().toString();
  }

  @Nullable
  private SonarLintLanguage getFileLanguage(IProject project, IFile file) {
    var location = file.getLocation();
    if (location == null) {
      return null;
    }

    var contentType = contentTypeResolver.apply(project, location.toOSString());

    if (contentType == null) {
      return null;
    }
    switch (contentType.getId()) {
      case CCorePlugin.CONTENT_TYPE_CHEADER:
      case CCorePlugin.CONTENT_TYPE_CSOURCE:
        return SonarLintLanguage.C;
      case CCorePlugin.CONTENT_TYPE_CXXHEADER:
      case CCorePlugin.CONTENT_TYPE_CXXSOURCE:
        return SonarLintLanguage.CPP;
      default:
        return null;
    }
  }

  private static Path createJsonFile(Path workDir, String content) throws IOException {
    var jsonFilePath = workDir.resolve(BUILD_WRAPPER_OUTPUT_FILENAME);
    Files.createDirectories(workDir);
    Files.write(jsonFilePath, content.getBytes(BUILD_WRAPPER_OUTPUT_CHARSET));
    return jsonFilePath;
  }

  @Nullable
  public SonarLintLanguage language(IFile iFile) {
    return getFileLanguage(iFile.getProject(), iFile);
  }

  public Set<IPath> getExcludedPaths(IProject project) {
    var exclusions = new HashSet<IPath>();
    var projectPath = project.getFullPath().makeAbsolute().toOSString();

    try {
      // 1) Check whether this is a CDT project
      var cProject = CoreModel.getDefault().getProjectDescription(project, false);
      if (cProject == null) {
        return exclusions;
      }

      // 2) Iterate over all the different configurations, e.g. Debug or Release
      for (var config : cProject.getConfigurations()) {
        var configData = config.getConfigurationData();
        if (configData == null) {
          continue;
        }

        var buildData = configData.getBuildData();
        if (buildData == null) {
          continue;
        }

        // 3) Iterate over all the output directories as there can be multiple ones per configuration
        for (var outputDirectory : buildData.getOutputDirectories()) {
          // On Windows, the paths are denoted with a backslash but when we create the "org.eclipse.core.runtime.Path"
          // it is correctly changed to a forward slash (used as an identifier as these are actually not paths in the
          // context of Eclipse but resource identifiers, looking like paths from an outside perspective but are always
          // concatenated via a forward slash, no matter the environment). When we want to test whether the build
          // directory equals the project directory reference or the base directory we replace it for easier checking.
          //
          // Let's say the project (generated by CMake with the generator "Eclipse CDT4 - Unix Makefiles" for the build
          // type "Debug" directly inside the project directory), then the following information is given:
          // - project name is "CMakeProject-Debug@CMakeProject"
          // - project directory reference is "CMakeProject-Debug@CMakeProject"
          // - base directory "/" that is also the same as the build output directory "/"
          // => We therefore cannot exclude the build directory which is the base directory from indexing!
          var outputDirectoryPath = outputDirectory.getFullPath().makeAbsolute().toOSString();
          outputDirectoryPath = outputDirectoryPath.replace("\\", "/");
          var localProjectPath = projectPath.replace("\\", "/");

          if (!localProjectPath.equals(outputDirectoryPath) && !"/".equals(outputDirectoryPath)) {
            // Only when the output directory is not the project directory we keep it. For example CMake projects can
            // generate CDT projects where the output is the project directory, this case should not be taken into
            // account and is up to the user. By default we will exclude compilation output when it is somewhere in a
            // sub-directory, e.g. "Debug" or "Release"!
            SonarLintLogger.get().traceIdeMessage("[CdtUtils#getExcludedPaths] Build output directory '"
              + outputDirectoryPath + "' does not match the project directory reference '" + localProjectPath
              + "' or the base directory at '/' for '" + project.getName() + "'. We exclude this from indexing!");
            exclusions.add(org.eclipse.core.runtime.Path.fromOSString(
              "/" + project.getName() + "/" + outputDirectory.getFullPath()));
          } else {
            SonarLintLogger.get().traceIdeMessage("[CdtUtils#getExcludedPaths] Build output directory '"
              + outputDirectoryPath + "' matches the project directory reference '" + localProjectPath
              + "' or the base directory at '/' for '" + project.getName()
              + "'. We therefore cannot exclude this from indexing!");
          }
        }
      }
    } catch (Exception err) {
      SonarLintLogger.get().error("Error while getting the exclusions of project '" + project.getName()
        + "' based on CDT!", err);
    }

    SonarLintLogger.get().traceIdeMessage("[CdtUtils#getExcludedPaths] The following paths have been excluded from "
      + "indexing for the project at '" + projectPath + "': "
      + String.join(", ", exclusions.stream().map(Object::toString).collect(Collectors.toList())));

    return exclusions;
  }
}
