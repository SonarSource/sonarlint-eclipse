/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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

package org.sonar.ide.eclipse.core.internal.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectRequest;
import org.sonar.ide.eclipse.core.internal.remote.EclipseSonar;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

/**
 * Project builder for {@link SonarNature} projects.
 *
 * @author Hemantkumar Chigadani
 */
@SuppressWarnings("nls")
public class SonarBuilder extends IncrementalProjectBuilder
{

  private final BuildWatcher watacher;

  /**
   * Builder for projects with nature {@link SonarNature}
   */
  public static final String BUILDER_ID = "org.sonar.ide.eclipse.core.sonarBuilder";

  /**
   * Shared builder request executor to avoid to many threads being spooned during application start-up.
   */
  private final static ForkJoinPool forkJoinPool = new ForkJoinPool(2);

  /**
   * Sonar project builder.
   */
  public SonarBuilder() {
    watacher = new BuildWatcher();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor) throws CoreException
  {
    final IProject project = getProject();
    final SonarCorePlugin corePlugin = SonarCorePlugin.getDefault();
    if (project == null || !project.isAccessible() || !SonarNature.hasSonarNature(project) || checkOffline(project)) {
      corePlugin.debug(project.getName() + "is not accessible.\n");
      return new IProject[0];
    }

    // CLEAN_BUILD is ignored

    final long currentTimeMillis = System.currentTimeMillis();
    if (kind == FULL_BUILD)
    {
      corePlugin.debug("Full build has been triggered to Sonar project : " + project.getName() + "\n");
      fullBuild(project);
    }
    else if (kind == AUTO_BUILD || kind == INCREMENTAL_BUILD)
    {
      corePlugin.debug("Incremenatal build has been triggered on Sonar project : " + project.getName() + "\n");
      incrementalBuild(monitor, project);

    }

    corePlugin.debug("Time taken to process build request '" + project.getName() + "' : ->" +
      (System.currentTimeMillis() - currentTimeMillis));

    return null;
  }

  /**
   * @param project
   * @return <code>true</code> if Sonar server is not reachable for the given project.
   */
  private final boolean checkOffline(final IProject project) {
    boolean offline = true;
    final EclipseSonar eclipseSonar = EclipseSonar.getInstance(project);

    if (eclipseSonar != null) {
      final ISonarServer sonarServer = eclipseSonar.getSonarServer();
      final String version = sonarServer.getVersion();
      if (StringUtils.isNotBlank(version)) {
        offline = false;
      }
    }
    return offline;
  }

  /**
   * Incremental local analysis build request.
   * 
   * @param monitor
   * @param project
   * @throws CoreException
   */
  private void incrementalBuild(final IProgressMonitor monitor, final IProject project)
    throws CoreException {

    // Silence incremental build
    final IResourceDelta delta = getDelta(project);
    if (delta.getKind() != IResourceDelta.NO_CHANGE)
    {
      final List<IResource> deltaChildren = filterDeltChangedFolders(delta);
      SonarCorePlugin.getDefault().debug("Parent folder having delta changes : " + deltaChildren + "\n");
      if (!deltaChildren.isEmpty())
      {
        scheduleIncrementalAnalysis(monitor, project, deltaChildren);
      }
    }
  }

  /**
   * @param project
   */
  private void fullBuild(final IProject project) {

    final AnalyseProjectRequest request = new AnalyseProjectRequest(project);
    request.setForceFullPreview(true);
    watacher.submit(request);
  }

  /**
   * @param delta All changed resources(files and folders) related to a project
   * @return Filtered parent folders of changed resource files.
   * @throws CoreException
   */
  private List<IResource> filterDeltChangedFolders(final IResourceDelta delta) throws CoreException
  {
    final List<IResource> deltaChildren = new ArrayList<IResource>();
    delta.accept(new IResourceDeltaVisitor()
    {

      @Override
      public boolean visit(final IResourceDelta deltaRes) throws CoreException
      {
        final IResource resource = deltaRes.getResource();
        final boolean changed = deltaRes.getKind() != IResourceDelta.NO_CHANGE;
        if (resource.getType() == IResource.FILE && changed)
        {
          SonarCorePlugin.getDefault().debug("File having changes :" + resource.getFullPath().toString() + "\n");
          deltaChildren.add(resource.getParent());
        }
        return true;
      }
    });
    return deltaChildren;
  }

  /**
   * @param monitor
   * @param project
   * @param deltaChildren
   */
  private void scheduleIncrementalAnalysis(final IProgressMonitor monitor, final IProject project, final List<IResource> deltaChildren)
  {
    final EclipseSonar eclipseSonar = EclipseSonar.getInstance(project);
    if (eclipseSonar != null) {

      Runnable prepareRequest = new Runnable() {
        @Override
        public void run() {
          try
          {
            // Local full-preview mode analysis job
            final AnalyseProjectRequest request = new AnalyseProjectRequest(project);
            final ISonarServer sonarServer = eclipseSonar.getSonarServer();
            final String version = sonarServer.getVersion();
            SonarCorePlugin.getDefault().debug("Sonar server version used by Soanr builder for incremental analysis :" + version);
            final List<SonarProperty> sonarExtraArgs = buildCustomSonarCongProperties(monitor, project, deltaChildren, version);
            // Found delta resources on the default Sonar project configuration paths
            if (!sonarExtraArgs.isEmpty())
            {
              request.setExtraProps(sonarExtraArgs);
              watacher.submit(request);
            }
          } catch (final Exception exception)
          {
            SonarCorePlugin.getDefault().debug(exception.getMessage());
          }
        }
      };
      forkJoinPool.submit(prepareRequest);
    }
  }

  /**
   * Update Sonar configuration properties with just changed delta resources.
   * This is helps in performance improvement to look for only changed resources.
   *
   * @param monitor
   * @param project
   * @param deltaChildren
   * @param version
   * @return
   */
  private ArrayList<SonarProperty> buildCustomSonarCongProperties(final IProgressMonitor monitor, final IProject project,
    final List<IResource> deltaChildren, final String version)
  {
    // Default Sonar configuration properties based on project configuration.
    final Properties deaultSonarConfProperties = new Properties();
    ConfiguratorUtils.configure(project, deaultSonarConfProperties, version, monitor);
    // TODO Global/Sonar project extra sonar properties to be used for overwriting,to get final defaults.

    // Filter and overwrite Sonar configuration properties based on the delta resources located on the respective default paths
    final ArrayList<SonarProperty> sonarExtraArgs = new ArrayList<SonarProperty>();
    for (final String sonarConfProperty : SonarConfiguratorProperties.PROPERTIES_LIST)
    {
      final String pathsForProperty = deaultSonarConfProperties.getProperty(sonarConfProperty);
      if (StringUtils.isNotBlank(pathsForProperty))
      {
        final List<String> deltaResourcesOnPaths = new ArrayList<String>();
        for (final IResource iResource : deltaChildren)
        {
          final String absoluteResourcePathString = ResourceUtils.getAbsolutePath(iResource.getFullPath());
          final String[] stringTokens = StringUtils.split(pathsForProperty, SonarProperties.SEPARATOR);
          for (final String projectSpecificPathString : stringTokens)
          {
            final String absoluteProjectSpecificPath = ResourceUtils.getAbsolutePath(Path.fromOSString(projectSpecificPathString));
            if (absoluteResourcePathString.startsWith(absoluteProjectSpecificPath))
            {
              deltaResourcesOnPaths.add(absoluteResourcePathString);
            }

          }
        }
        // New value with only changed delta resources.
        final String join = StringUtils.join(deltaResourcesOnPaths, SonarProperties.SEPARATOR);
        if (!join.isEmpty())
        {
          sonarExtraArgs.add(new SonarProperty(sonarConfProperty, join));
        }
      }
    }
    return sonarExtraArgs;
  }

}
