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
package org.sonar.ide.eclipse.core.internal;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.sonar.ide.eclipse.core.internal.builder.SonarBuilder;

public class SonarNature implements IProjectNature {

  public static final String NATURE_ID = SonarCorePlugin.PLUGIN_ID + ".sonarNature";

  private IProject project;

  @Override
  public void configure() throws CoreException {
    // Nothing to do
  }

  @Override
  public void deconfigure() throws CoreException {
    // Nothing to do
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(final IProject project) {
    this.project = project;
  }

  public static boolean hasSonarNature(final IProject project) {
    try {
      return project.hasNature(NATURE_ID);
    } catch (final CoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
      return false;
    }
  }

  public static void enableNature(final IProject project) throws CoreException
  {
    final IProjectDescription description = project.getDescription();
    addSonarNature(description);
    addSonarBuilder(description);

    setDescription(project, description);
  }

  public static void disableNature(final IProject project) throws CoreException
  {
    project.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);

    final IProjectDescription description = project.getDescription();
    removeSonarNature(description);
    removeSonarBuilder(description);

    setDescription(project, description);
  }

  /**
   * @param description
   */
  private static void addSonarNature(final IProjectDescription description)
  {
    final String[] prevNatures = description.getNatureIds();
    final String[] newNatures = new String[prevNatures.length + 1];
    System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
    newNatures[0] = NATURE_ID;
    description.setNatureIds(newNatures);
  }

  /**
   * @param description
   */
  private static void removeSonarNature(final IProjectDescription description)
  {
    final List<String> newNatures = Lists.newArrayList();
    for (final String natureId : description.getNatureIds())
    {
      if (!NATURE_ID.equals(natureId))
      {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
  }

  /**
   * @param project
   * @param description
   * @throws CoreException
   */
  static void setDescription(final IProject project, final IProjectDescription description) throws CoreException
  {
    project.setDescription(description, null);
    // Refresh project after editing the natures list.
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable()
    {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException
      {
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

      }
    }, new NullProgressMonitor());
  }

   /**
    * @param description
    */
  static void addSonarBuilder(final IProjectDescription description)
   {
      final ICommand[] oldBuildSpec = description.getBuildSpec();
      final List< ICommand > oldBuilderCommands = Arrays.asList( oldBuildSpec );
      final List< ICommand > sonarBuilderCommands = getSonarCommands( oldBuilderCommands );
      final List< ICommand > newBuilderCommands = new ArrayList< ICommand >( oldBuilderCommands );

      if ( sonarBuilderCommands.isEmpty() )
      {
         final ICommand newCommand = description.newCommand();
         newCommand.setBuilderName( SonarBuilder.BUILDER_ID );
         // Add at last
         newBuilderCommands.add( newCommand );
      }
      else
      {
         newBuilderCommands.removeAll( sonarBuilderCommands );
         newBuilderCommands.add( sonarBuilderCommands.get( 0 ) );
      }

      // Commit the spec change into the project
      description.setBuildSpec( newBuilderCommands.toArray( new ICommand[ 0 ] ) );
   }
   /**
    * @param description
    */
   private static void removeSonarBuilder( final IProjectDescription description )
   {
      final ICommand[] oldBuildSpec = description.getBuildSpec();
      final List< ICommand > oldBuilderCommands = Arrays.asList( oldBuildSpec );
      final List< ICommand > sonarBuildercommands = getSonarCommands( oldBuilderCommands );
      final List< ICommand > newBuilderCommands = new ArrayList< ICommand >( oldBuilderCommands );
      newBuilderCommands.removeAll( sonarBuildercommands );

      // Commit the spec change into the project
      description.setBuildSpec( newBuilderCommands.toArray( new ICommand[ 0 ] ) );
   }

   /**
    * Find the specific Java command amongst the given build spec
    * and return its index or -1 if not found.
    */
   private static List< ICommand > getSonarCommands( final List< ICommand > buildSpec )
   {

      final List< ICommand > list = new ArrayList< ICommand >();
      for ( int i = 0; i < buildSpec.size(); ++i )
      {
         final ICommand iCommand = buildSpec.get( i );
         final String builderName = iCommand.getBuilderName();
         if ( builderName.equals( SonarBuilder.BUILDER_ID ) )
         {
            list.add( iCommand );
         }
      }
      return list;
   }
}
