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
package org.sonar.ide.eclipse.m2e;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarKeyUtils;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;

import java.util.Collection;

public class SonarProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    if (!SonarNature.hasSonarNature(request.getProject())) {
      SonarMavenInfos infos = new SonarMavenInfos(request.getMavenProjectFacade());
      Collection<SonarServer> servers = SonarCorePlugin.getServersManager().getServers();
      if (servers.size() > 0) {
        String url = servers.iterator().next().getUrl();
        SonarCorePlugin.createSonarProject(request.getProject(), url, infos.getKey(), false);
      }
    }
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) {
    IProject project = event.getMavenProject().getProject();
    if (SonarNature.hasSonarNature(project)) {
      SonarMavenInfos oldInfos = new SonarMavenInfos(event.getOldMavenProject());
      SonarMavenInfos newInfos = new SonarMavenInfos(event.getMavenProject());
      if (oldInfos.equals(newInfos)) {
        return;
      }
      // Now check that Sonar configuration was in sync with old infos to not override user defined association
      SonarProject sonarProject = SonarProject.getInstance(project);
      if (sonarProject.getKey().equals(oldInfos.getKey())) {
        sonarProject.setKey(newInfos.getKey());
        sonarProject.save();
      }
    }
  }

  private static class SonarMavenInfos {
    private String groupId;
    private String artifactId;
    private String branch;

    public SonarMavenInfos(IMavenProjectFacade facade) {
      groupId = facade.getArtifactKey().getGroupId();
      artifactId = facade.getArtifactKey().getArtifactId();
      Object branchObj = facade.getMavenProject().getProperties().get(SonarProperties.PROJECT_BRANCH_PROPERTY);
      branch = branchObj != null ? branchObj.toString() : null;
    }

    public String getKey() {
      return SonarKeyUtils.projectKey(groupId, artifactId, branch);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SonarMavenInfos) {
        SonarMavenInfos other = (SonarMavenInfos) obj;
        return new EqualsBuilder()
            .append(groupId, other.groupId)
            .append(artifactId, other.artifactId)
            .append(branch, other.branch)
            .isEquals();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(7, 43)
          .append(groupId)
          .append(artifactId)
          .append(branch)
          .hashCode();
    }
  }
}
