/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.erlide.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.erlide.core.model.root.ErlModelManager;
import org.erlide.core.model.root.IErlModel;
import org.erlide.core.model.root.IErlProject;
import org.erlide.core.model.util.ErlideUtil;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;

import java.util.Properties;

public class ErlangProjectConfigurator extends ProjectConfigurator {

    @Override
    public boolean canConfigure(IProject project) {
        return ErlideUtil.hasErlangNature(project);
    }

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
        IProject project = request.getProject();
        IErlModel model = ErlModelManager.getErlangModel();
        IErlProject erlProject = model.getErlangProject(project);
        configureErlangProject(project, erlProject, request.getSonarProjectProperties());
    }

    private void configureErlangProject(IProject project, IErlProject erlProject, Properties sonarProjectProperties) {
        sonarProjectProperties.setProperty(SonarConfiguratorProperties.PROJECT_LANGUAGE_PROPERTY, "erlang");
        for (String path : SonarErlIdePlugin.getSourceFolders(project.getLocation().toOSString(), erlProject)) {
            //TODO: dirty hack, there is no test property in erlide so every test directory is in the src paths, we add all path as
            //test path if it ends with test or tests
            if(path.matches("tests?[/\\\\]*$")){
                appendProperty(sonarProjectProperties, SonarConfiguratorProperties.TEST_DIRS_PROPERTY, getAbsolutePath(new Path(path)));
            } else {
                appendProperty(sonarProjectProperties, SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, getAbsolutePath(new Path(path)));
            }
        }
    }
}
