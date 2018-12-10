/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.adapter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectAdapterParticipant;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSonarLintAdapterFactoryTest extends SonarTestCase {

  private Path tempProjectBasedir;

  @Before
  public void prepare() throws IOException {
    tempProjectBasedir = new Path("temp");
  }

  @Test
  public void mimic_cobol_ide() throws IOException {
    CobolTempProject tempProject = new CobolTempProject(tempProjectBasedir);
    IPath filePath = tempProjectBasedir.append("MyProg.cbl");
    CobolFile cobolFile = new CobolFile(filePath, tempProject);

    ISonarLintFile sonarLintFile = Adapters.adapt(cobolFile, ISonarLintFile.class);

    assertThat(sonarLintFile).isNotNull();
    assertThat(sonarLintFile.getProject().getName()).isEqualTo("module");
    assertThat(sonarLintFile.getProject().supportsFullAnalysis()).isFalse();

  }

  public static class CobolSLProjectParticipant implements ISonarLintProjectAdapterParticipant {
    @Override
    public boolean exclude(IProject project) {
      // Don't want temp project to be visible to SonarLint
      return project instanceof CobolTempProject;
    }
  }

  public static class CobolSLFileAdapterParticipant implements ISonarLintFileAdapterParticipant {

    @Override
    public ISonarLintFile adapt(IFile file) {
      if (file instanceof CobolFile) {
        // lookup the logical module this file belongs to
        CobolModule module = new CobolModule(new Path("module"));
        return new CobolFileAdapter((CobolFile) file, module);
      }
      return null;
    }

  }

  public static class CobolFileAdapter implements ISonarLintFile {

    private final CobolFile file;
    private final CobolModule logicalModule;

    public CobolFileAdapter(CobolFile file, CobolModule logicalModule) {
      this.file = file;
      this.logicalModule = logicalModule;
    }

    @Override
    public ISonarLintProject getProject() {
      return new CobolModuleAdapter(logicalModule);
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public IResource getResource() {
      return file;
    }

    @Override
    public IDocument getDocument() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getProjectRelativePath() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Charset getCharset() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  public static class CobolModuleAdapter implements ISonarLintProject {

    private final CobolModule module;

    public CobolModuleAdapter(CobolModule module) {
      this.module = module;
    }

    @Override
    public ISonarLintProject getProject() {
      return this;
    }

    @Override
    public boolean supportsFullAnalysis() {
      return false;
    }

    @Override
    public IResource getResource() {
      return module;
    }

    @Override
    public String getName() {
      return ((IContainer) module).getName();
    }

    @Override
    public java.nio.file.Path getWorkingDir() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean exists(String relativeFilePath) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Object getObjectToNotify() {
      return module;
    }

    @Override
    public Collection<ISonarLintFile> files() {
      // TODO Auto-generated method stub
      return null;
    }

  }

  public static class CobolFile extends File {

    private CobolTempProject cobolTempProject;

    protected CobolFile(IPath path, CobolTempProject project) {
      super(path, (Workspace) workspace);
      this.cobolTempProject = project;
    }

    @Override
    public IProject getProject() {
      return cobolTempProject;
    }

    @Override
    public boolean exists() {
      return true;
    }

  }

  /**
   * Temp Project is the physical place where all Cobol files coming from the mainframe are stored
   */
  public static class CobolTempProject extends org.eclipse.core.internal.resources.Project implements IProject {

    protected CobolTempProject(IPath path) {
      super(path, (Workspace) workspace);
    }

  }

  /**
   * CobolModule is a logical concept in Cobol IDE. This looks like a project, but it doesn't adapt to a project
   */
  public static class CobolModule extends Container implements IContainer {

    protected CobolModule(IPath path) {
      super(path, (Workspace) workspace);
    }

    @Override
    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
      return null;
    }

    @Override
    public int getType() {
      return IResource.FOLDER;
    }

  }

}
