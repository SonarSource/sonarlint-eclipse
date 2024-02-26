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
import java.util.List;
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
import org.sonarlint.eclipse.core.internal.jobs.TestFileClassifier;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.DidUpdateFileSystemParams;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;

import static java.util.stream.Collectors.toList;

public class FileSystemSynchronizer implements IResourceChangeListener {

  private static final String SONAR_SCANNER_CONFIG_FILENAME = "sonar-project.properties";
  private static final String AUTOSCAN_CONFIG_FILENAME = ".sonarcloud.properties";

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
    if (slFile.getName().endsWith(SONAR_SCANNER_CONFIG_FILENAME) || slFile.getName().endsWith(AUTOSCAN_CONFIG_FILENAME)) {
      fileContent = slFile.getDocument().get();
    }
    return new ClientFileDto(slFile.uri(), Paths.get(slFile.getProjectRelativePath()), configScopeId, TestFileClassifier.get().isTest(slFile),
      slFile.getCharset().name(), fsPath, fileContent);
  }

}
