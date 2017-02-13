/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfoProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;

public class CProjectConfigurator extends ProjectConfigurator {
  private static final String CFAMILY_USE_CACHE = "sonar.cfamily.useCache";
  private static final String BUILD_WRAPPER_OUTPUT_PROP = "sonar.cfamily.build-wrapper-output";
  private static final String BUILD_WRAPPER_OUTPUT_FILENAME = "build-wrapper-dump.json";
  private static final Charset BUILD_WRAPPER_OUTPUT_CHARSET = StandardCharsets.UTF_8;
  private final BuildWrapperJsonFactory jsonFactory;
  private final CCorePlugin cCorePlugin;
  private final Predicate<IFile> fileValidator;
  private final SonarLintLogger logger;
  private final FilePathResolver filePathResolver;
  private final BiFunction<IProject, Path, IContentType> contentTypeResolver;

  public CProjectConfigurator() {
    this(new BuildWrapperJsonFactory(), CCorePlugin.getDefault(), CoreModel::isTranslationUnit,
      (project, path) -> CCorePlugin.getContentType(project, path.toString()), SonarLintLogger.get(),
      new FilePathResolver());
  }

  public CProjectConfigurator(BuildWrapperJsonFactory jsonFactory, CCorePlugin cCorePlugin, Predicate<IFile> fileValidator,
    BiFunction<IProject, Path, IContentType> contentTypeResolver, SonarLintLogger logger, FilePathResolver filePathResolver) {
    this.jsonFactory = jsonFactory;
    this.cCorePlugin = cCorePlugin;
    this.fileValidator = fileValidator;
    this.logger = logger;
    this.filePathResolver = filePathResolver;
    this.contentTypeResolver = contentTypeResolver;
  }

  @Override
  public boolean canConfigure(IProject project) {
    return SonarCdtPlugin.hasCNature(project);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    Collection<IFile> filesToAnalyze = request.getFilesToAnalyze()
      .stream().filter(fileValidator)
      .collect(Collectors.toList());

    try {
      Collection<ConfiguredFile> configuredFiles = configureCProject(request.getProject(), filesToAnalyze);
      Path jsonPath = writeJson(request.getProject(), configuredFiles);
      logger.debug("Wrote build info to: " + jsonPath.toString());
      request.getSonarProjectProperties().put(CFAMILY_USE_CACHE, Boolean.FALSE.toString());
      request.getSonarProjectProperties().put(BUILD_WRAPPER_OUTPUT_PROP, jsonPath.getParent().toString());
      configuredFiles.forEach(f -> request.getFileLanguages().put(f.file(), f.languageKey()));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  private static Path getProjectBaseDir(IProject project) {
    IPath projectLocation = project.getLocation();
    // In some infrequent cases the project may be virtual and don't have physical location
    return projectLocation != null ? projectLocation.toFile().toPath() : ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
  }

  private Collection<ConfiguredFile> configureCProject(IProject project, Collection<IFile> filesToAnalyze) {
    List<ConfiguredFile> files = new LinkedList<>();
    IScannerInfoProvider infoProvider = cCorePlugin.getScannerInfoProvider(project);

    for (IFile file : filesToAnalyze) {
      try {
        ConfiguredFile.Builder builder = new ConfiguredFile.Builder(file);

        Path path = filePathResolver.getPath(file);
        IScannerInfo fileInfo = infoProvider.getScannerInformation(file);
        String languageKey = getFileLanguage(project, file, path);

        builder.includes(fileInfo.getIncludePaths() != null ? fileInfo.getIncludePaths() : new String[0])
          .symbols(fileInfo.getDefinedSymbols() != null ? fileInfo.getDefinedSymbols() : Collections.emptyMap())
          .path(path.toString())
          .languageKey(languageKey);

        files.add(builder.build());
      } catch (CoreException e) {
        logger.error("Error building input file for SonarLint analysis: " + file.getName(), e);
      }
    }
    return files;

  }

  private Path writeJson(IProject project, Collection<ConfiguredFile> files) throws IOException {
    Path projectBaseDir = getProjectBaseDir(project);
    String json = jsonFactory.create(files, projectBaseDir.toString());
    return createJsonFile(filePathResolver.getWorkDir(), json);
  }

  @CheckForNull
  private String getFileLanguage(IProject project, IFile file, Path path) {
    IContentType contentType = contentTypeResolver.apply(project, path);

    if (contentType == null) {
      return null;
    }
    switch (contentType.getId()) {
      case CCorePlugin.CONTENT_TYPE_CHEADER:
      case CCorePlugin.CONTENT_TYPE_CSOURCE:
        return "c";
      case CCorePlugin.CONTENT_TYPE_CXXHEADER:
      case CCorePlugin.CONTENT_TYPE_CXXSOURCE:
        return "cpp";
      default:
        return null;
    }
  }

  private static Path createJsonFile(Path workDir, String content) throws IOException {
    Path jsonFilePath = workDir.resolve(BUILD_WRAPPER_OUTPUT_FILENAME);
    Files.createDirectories(workDir);
    Files.write(jsonFilePath, content.getBytes(BUILD_WRAPPER_OUTPUT_CHARSET));
    return jsonFilePath;
  }
}
