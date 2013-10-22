/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarKeyUtils;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class SonarProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    SonarMavenInfos infos = new SonarMavenInfos(request.getMavenProjectFacade(), monitor);
    if (!SonarNature.hasSonarNature(request.getProject())) {
      Collection<ISonarServer> servers = SonarCorePlugin.getServersManager().getServers();
      if (!servers.isEmpty()) {
        // Take the first configured server
        String url = servers.iterator().next().getUrl();
        SonarProject sonarProject = SonarCorePlugin.createSonarProject(request.getProject(), url, infos.getKey());
        if (!infos.getSonarProperties().isEmpty()) {
          sonarProject.setExtraProperties(toSonarProperty(infos.getSonarProperties()));
          sonarProject.save();
        }
      }
    }
  }

  private static List<SonarProperty> toSonarProperty(Map<String, String> properties) {
    List<SonarProperty> extraArguments = new ArrayList<SonarProperty>();
    for (Entry<String, String> property : properties.entrySet()) {
      extraArguments.add(new SonarProperty(property.getKey(), property.getValue()));
    }
    return extraArguments;
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade newProject = event.getMavenProject();
    if (newProject == null) {
      // project was removed, nothing to configure
      return;
    }

    IProject project = newProject.getProject();
    if (SonarNature.hasSonarNature(project)) {
      IMavenProjectFacade oldProject = event.getOldMavenProject();
      // if old == null -> project was added
      if (oldProject == null) {
        mavenProjectAdded(project, newProject, monitor);
      }
      else {
        mavenProjectUpdated(project, newProject, oldProject, monitor);
      }
    }
  }

  private void mavenProjectUpdated(IProject project, IMavenProjectFacade newProject, IMavenProjectFacade oldProject, IProgressMonitor monitor) throws CoreException {
    SonarMavenInfos oldInfos = new SonarMavenInfos(oldProject, monitor);
    SonarMavenInfos newInfos = new SonarMavenInfos(newProject, monitor);
    if (oldInfos.equals(newInfos)) {
      return;
    }
    // Now check that Sonar configuration was in sync with old infos to not override user defined association
    SonarProject sonarProject = SonarProject.getInstance(project);
    if (sonarProject.getKey().equals(oldInfos.getKey())) {
      sonarProject.setKey(newInfos.getKey());
      sonarProject.save();
    }
    // Now check that Sonar project properties were in sync with old info to not override used defined properties
    if (propertiesEqualsArgments(sonarProject.getExtraProperties(), oldInfos.getSonarProperties())) {
      Map<String, String> diffs = diff(oldInfos.getSonarProperties(), newInfos.getSonarProperties());
      update(sonarProject.getExtraProperties(), diffs);
      sonarProject.save();
    }

  }

  private void mavenProjectAdded(IProject project, IMavenProjectFacade newProject, IProgressMonitor monitor) throws CoreException {
    SonarMavenInfos newInfos = new SonarMavenInfos(newProject, monitor);
    SonarProject sonarProject = SonarProject.getInstance(project);
    sonarProject.setKey(newInfos.getKey());
    sonarProject.getExtraProperties().clear();
    update(sonarProject.getExtraProperties(), newInfos.getSonarProperties());

    sonarProject.save();
  }

  private void update(List<SonarProperty> extraArguments, Map<String, String> diffs) {
    for (Entry<String, String> diff : diffs.entrySet()) {
      int position = removeIfPresent(extraArguments, diff.getKey());
      if (diff.getValue() != null) {
        if (position == -1) {
          extraArguments.add(new SonarProperty(diff.getKey(), diff.getValue()));
        }
        else {
          extraArguments.add(position, new SonarProperty(diff.getKey(), diff.getValue()));
        }
      }
    }
  }

  private int removeIfPresent(List<SonarProperty> extraArguments, String key) {
    Iterator<SonarProperty> i = extraArguments.iterator();
    int position = -1;
    while (i.hasNext()) {
      SonarProperty arg = i.next();
      position++;
      if (arg.getName().equals(key)) {
        i.remove();
      }
    }
    return position;
  }

  private static class SonarMavenInfos {
    private String groupId;
    private String artifactId;
    private String branch;
    private Map<String, String> sonarProperties;

    public SonarMavenInfos(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
      groupId = facade.getArtifactKey().getGroupId();
      artifactId = facade.getArtifactKey().getArtifactId();
      Properties properties = facade.getMavenProject(monitor).getProperties();
      Object branchObj = null;
      sonarProperties = new HashMap<String, String>();
      for (Entry<Object, Object> property : properties.entrySet()) {
        if (property.getKey().equals(SonarProperties.PROJECT_BRANCH_PROPERTY)) {
          branchObj = property.getValue();
        }
        else if (property.getKey().toString().startsWith("sonar.")) {
          sonarProperties.put(property.getKey().toString(), ObjectUtils.toString(property.getValue()));
        }
      }
      branch = branchObj != null ? branchObj.toString() : null;
    }

    public String getKey() {
      return SonarKeyUtils.projectKey(groupId, artifactId, branch);
    }

    public Map<String, String> getSonarProperties() {
      return sonarProperties;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SonarMavenInfos) {
        SonarMavenInfos other = (SonarMavenInfos) obj;
        return new EqualsBuilder()
          .append(groupId, other.groupId)
          .append(artifactId, other.artifactId)
          .append(branch, other.branch)
          .isEquals()
          && diff(sonarProperties, other.sonarProperties).isEmpty();
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

  private static Map<String, String> diff(Map<String, String> props1, Map<String, String> props2) {
    Map<String, String> diff = new HashMap<String, String>(props2);
    for (Entry<String, String> prop1 : props1.entrySet()) {
      if (props2.containsKey(prop1.getKey())) {
        if (props2.get(prop1.getKey()).equals(props1.get(prop1.getKey()))) {
          diff.remove(prop1.getKey());
        }
      }
      else {
        // Null value means the property was removed
        diff.put(prop1.getKey(), null);
      }
    }
    return diff;
  }

  /**
   * Verify that all properties are present in the SonarProperty list with same value
   * @param args
   * @param properties
   * @return
   */
  private static boolean propertiesEqualsArgments(List<SonarProperty> args, Map<String, String> properties) {
    for (Entry<String, String> prop : properties.entrySet()) {
      if (!args.contains(new SonarProperty(prop.getKey(), prop.getValue()))) {
        return false;
      }
    }
    return true;
  }
}
