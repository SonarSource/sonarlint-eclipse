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
package org.sonar.ide.eclipse.runner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

/** HelloWorldAction is a simple example of using an
 * action set to extend the Eclipse Workbench with a menu
 * and toolbar action that prints the "Hello World" message.
 */
public class SonarEclipseRunner {

  private SonarEclipseRunner() {
    // Utility class
  }

  /** Run the action. Display the Hello World message
   */
  public static IStatus run(IProject project, Properties props, final IProgressMonitor monitor) {

    File tmpSonarRunnerJarPath = null;
    try {
      tmpSonarRunnerJarPath = extractSonarRunnerJar();

      IVMRunner vmRunner = getVMRunner(project);
      if (vmRunner == null) {
        return new Status(Status.ERROR, SonarRunnerPlugin.PLUGIN_ID, "No usable JVM found");
      }

      VMRunnerConfiguration vmConfig = new VMRunnerConfiguration("org.sonar.runner.Main", new String[] {tmpSonarRunnerJarPath.toString()});
      vmConfig.setWorkingDirectory(project.getLocation().toOSString());
      vmConfig.setVMArguments(prepareVMArgs(props));

      final ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null) {

        @Override
        public void addProcess(IProcess process) {
          new StreamListener(process.getStreamsProxy().getErrorStreamMonitor()) {
            @Override
            protected void write(String text) {
              SonarRunnerPlugin.getDefault().error(text);
            }
          };
          new StreamListener(process.getStreamsProxy().getOutputStreamMonitor()) {
            @Override
            protected void write(String text) {
              SonarRunnerPlugin.getDefault().info(text);
            }
          };
          super.addProcess(process);
        }
      };

      // Start process
      vmRunner.run(vmConfig, launch, monitor);

      // Wait for process to complete
      while (!launch.isTerminated()) {
        if (monitor.isCanceled()) {
          launch.terminate();
          return Status.CANCEL_STATUS;
        }
        else {
          Thread.sleep(100);
        }
      }

      // Check exit code
      if (launch.getProcesses()[0].getExitValue() == 0) {
        return Status.OK_STATUS;
      }
      else {
        return new Status(Status.ERROR, SonarRunnerPlugin.PLUGIN_ID, "Check Sonar console for details");
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      FileUtils.deleteQuietly(tmpSonarRunnerJarPath);
    }

  }

  private static IVMRunner getVMRunner(IProject project) throws CoreException {
    IVMInstall vmInstall = null;
    if (project instanceof IJavaProject) {
      vmInstall = JavaRuntime.getVMInstall((IJavaProject) project);
    }
    if (vmInstall == null) {
      vmInstall = JavaRuntime.getDefaultVMInstall();
    }
    if (vmInstall == null) {
      return null;
    }

    return vmInstall.getVMRunner(ILaunchManager.RUN_MODE);
  }

  private static String[] prepareVMArgs(Properties props) {
    ArrayList<String> args = new ArrayList<String>();
    for (Object key : props.keySet()) {
      args.add("-D" + key + "=" + props.getProperty(key.toString()));
    }
    return args.toArray(new String[args.size()]);
  }

  private static File extractSonarRunnerJar() throws IOException {
    InputStream is = null;
    OutputStream os = null;
    try {
      URL jarRunner = SonarRunnerPlugin.getDefault().getBundle().getEntry("/jars/sonar-runner-2.0.jar");

      File tmpSonarRunnerJarPath = File.createTempFile("sonar-runner", ".jar");
      is = jarRunner.openStream();
      os = new FileOutputStream(tmpSonarRunnerJarPath);
      IOUtils.copy(is, os);
      return tmpSonarRunnerJarPath;
    } finally {
      IOUtils.closeQuietly(os);
      IOUtils.closeQuietly(is);
    }
  }
}
