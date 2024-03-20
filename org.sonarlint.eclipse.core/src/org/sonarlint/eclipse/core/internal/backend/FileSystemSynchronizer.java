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
package org.sonarlint.eclipse.core.internal.backend;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.TestFileClassifier;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.DidUpdateFileSystemParams;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;

import static java.util.stream.Collectors.toList;

public class FileSystemSynchronizer implements IResourceChangeListener {

  private static final String SONAR_SCANNER_CONFIG_FILENAME = "sonar-project.properties";
  private static final String AUTOSCAN_CONFIG_FILENAME = ".sonarcloud.properties";
  public static final Pattern SONARLINT_JSON_REGEX = Pattern.compile("^\\.sonarlint/.*\\.json$", Pattern.CASE_INSENSITIVE);

  private final SonarLintRpcServer backend;

  FileSystemSynchronizer(SonarLintRpcServer backend) {
    this.backend = backend;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    var changedOrAddedFiles = new ArrayList<ISonarLintFile>();
    var removedFiles = new ArrayList<URI>();
    try {
      event.getDelta().accept(delta -> visitDeltaPostChange(delta, changedOrAddedFiles, removedFiles));
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }

    var job = new Job("SonarLint - Propagate FileSystem changes") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        var changedOrAddedDto = changedOrAddedFiles.stream()
          .map(f -> FileSystemSynchronizer.toFileDto(f, monitor))
          .collect(toList());
        backend.getFileService().didUpdateFileSystem(new DidUpdateFileSystemParams(removedFiles, changedOrAddedDto));

        // If the ".sonarlint/*.json" files were changed or added, we also have to inform all the sub-projects!
        var changedOrAddedSonarLintDto = changedOrAddedDto.stream()
          .filter(dto -> SONARLINT_JSON_REGEX.matcher(dto.getIdeRelativePath().toString()).find())
          .collect(toList());
        if (changedOrAddedSonarLintDto.isEmpty()) {
          return Status.OK_STATUS;
        }

        // We only check for the first files' project as it is not possible (I assume, or very unlikely) that more
        // than one SonarLint JSON file is changed / added per Eclipse IDE event!
        var projectOpt = SonarLintUtils.tryResolveProject(changedOrAddedSonarLintDto.get(0).getConfigScopeId());
        if (projectOpt.isEmpty()) {
          return Status.OK_STATUS;
        }
        var project = projectOpt.get();

        var subProjects = new HashSet<ISonarLintProject>();
        for (var projectHistoryProvider : SonarLintExtensionTracker.getInstance().getProjectHierarchyProviders()) {
          if (projectHistoryProvider.partOfHierarchy(project)) {
            subProjects.addAll(projectHistoryProvider.getSubProjects(project));
          }
        }

        for (var subProject : subProjects) {
          var changedOrAddedSubProjectDto = changedOrAddedSonarLintDto.stream()
            .map(dto -> toSubProjectFileDto(subProject, dto))
            .collect(toList());
          backend.getFileService().didUpdateFileSystem(new DidUpdateFileSystemParams(List.of(), changedOrAddedSubProjectDto));
        }

        return Status.OK_STATUS;
      }
    };

    job.setSystem(true);
    job.schedule();
  }

  private static boolean visitDeltaPostChange(IResourceDelta delta, List<ISonarLintFile> changedOrAddedFiles, List<URI> removedFiles) {
    var res = delta.getResource();
    switch (delta.getKind()) {
      case IResourceDelta.ADDED:
        var slFile = Adapters.adapt(res, ISonarLintFile.class);
        if (slFile != null) {
          SonarLintLogger.get().debug("File added: " + slFile.getName());
          changedOrAddedFiles.add(slFile);
        }
        break;
      case IResourceDelta.REMOVED:
        var fileUri = res.getLocationURI();
        if (fileUri != null) {
          removedFiles.add(fileUri);
          SonarLintLogger.get().debug("File removed: " + fileUri);
        }
        break;
      case IResourceDelta.CHANGED:
        var changedSlFile = Adapters.adapt(res, ISonarLintFile.class);
        if (changedSlFile != null) {
          var interestingChangeForSlBackend = false;
          var flags = delta.getFlags();
          if ((flags & IResourceDelta.CONTENT) != 0) {
            interestingChangeForSlBackend = true;
            SonarLintLogger.get().debug("File content changed: " + changedSlFile.getName());
          }
          if ((flags & IResourceDelta.REPLACED) != 0) {
            interestingChangeForSlBackend = true;
            SonarLintLogger.get().debug("File content replaced: " + changedSlFile.getName());
          }
          if ((flags & IResourceDelta.ENCODING) != 0) {
            interestingChangeForSlBackend = true;
            SonarLintLogger.get().debug("File encoding changed: " + changedSlFile.getName());
          }
          if (interestingChangeForSlBackend) {
            changedOrAddedFiles.add(changedSlFile);
          }
        }
        break;
      default:
        break;
    }
    return true;
  }

  static ClientFileDto toFileDto(ISonarLintFile slFile, IProgressMonitor monitor) {
    var configScopeId = ConfigScopeSynchronizer.getConfigScopeId(slFile.getProject());
    Path fsPath;
    try {
      var fileStore = EFS.getStore(slFile.getResource().getLocationURI());
      var localFile = fileStore.toLocalFile(EFS.NONE, monitor);
      if (localFile != null) {
        fsPath = localFile.toPath().toAbsolutePath();
      } else {
        fsPath = null;
      }

    } catch (Exception e) {
      SonarLintLogger.get().debug("Error while looking for file path for file " + slFile, e);
      fsPath = null;
    }

    String fileContent = null;
    if (slFile.getName().endsWith(SONAR_SCANNER_CONFIG_FILENAME) || slFile.getName().endsWith(AUTOSCAN_CONFIG_FILENAME)
      || matchesSonarLintJson(slFile)) {
      fileContent = slFile.getDocument().get();
    }

    return new ClientFileDto(slFile.uri(), Paths.get(slFile.getProjectRelativePath()), configScopeId, TestFileClassifier.get().isTest(slFile),
      slFile.getCharset().name(), fsPath, fileContent);
  }

  /**
   *  This converts a ClientFileDto from a root project to sub-project one with a correct relative path, changing only
   *  it and the configScopeId (to the sub-project)!
   *
   *  @param project the sub-project which will be used for the relative path
   *  @param dto from the root project
   *  @return DTO for the sub-project
   */
  static ClientFileDto toSubProjectFileDto(ISonarLintProject project, ClientFileDto dto) {
    var currentDtoUri = Paths.get(dto.getUri());
    var projectUri = Paths.get(project.getResource().getLocationURI());
    var relativePath = projectUri.relativize(currentDtoUri);

    return new ClientFileDto(dto.getUri(), relativePath, ConfigScopeSynchronizer.getConfigScopeId(project),
      dto.isTest(), dto.getCharset(), dto.getFsPath(), dto.getContent());
  }

  /** This only gets the SonarLint JSON files for a specific project, instead of all of them! */
  public static Collection<ISonarLintFile> getSonarLintJsonFiles(ISonarLintProject project) {
    return project.files().stream()
      .filter(FileSystemSynchronizer::matchesSonarLintJson)
      .collect(toList());
  }

  /** This checks if a given ISonarLintFile matches the SonarLint JSON Regex! */
  private static boolean matchesSonarLintJson(ISonarLintFile file) {
    return SONARLINT_JSON_REGEX.matcher(file.getProjectRelativePath()).find();
  }
}
