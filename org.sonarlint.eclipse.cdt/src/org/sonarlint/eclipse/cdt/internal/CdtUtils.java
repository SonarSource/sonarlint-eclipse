/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
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
  private String getFileLanguage(IProject project, IFile file) {
    var location = file.getLocation();
    if (location == null) {
      return null;
    }

    IContentType contentType = contentTypeResolver.apply(project, location.toOSString());

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
    var jsonFilePath = workDir.resolve(BUILD_WRAPPER_OUTPUT_FILENAME);
    Files.createDirectories(workDir);
    Files.write(jsonFilePath, content.getBytes(BUILD_WRAPPER_OUTPUT_CHARSET));
    return jsonFilePath;
  }

  @Nullable
  public String language(IFile iFile) {
    return getFileLanguage(iFile.getProject(), iFile);
  }
}
