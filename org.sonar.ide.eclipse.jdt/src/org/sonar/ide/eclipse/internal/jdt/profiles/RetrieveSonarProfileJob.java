/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.jdt.profiles;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.internal.jdt.SonarJdtPlugin;
import org.sonar.ide.eclipse.internal.ui.jobs.AbstractRemoteSonarJob;
import org.sonar.ide.eclipse.jdt.profiles.ISonarRuleConverter;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.shared.profile.ProfileUtil;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
@SuppressWarnings("restriction")
public class RetrieveSonarProfileJob extends AbstractRemoteSonarJob {

  // ID from converter extension point
  private static final String CONVERTER_ID = SonarJdtPlugin.PLUGIN_ID + ".ruleconverter";

  public RetrieveSonarProfileJob() {
    super("Retrieve sonar profile");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (IProject project : projects) {
      if ( !monitor.isCanceled() && project.isAccessible()) {
        EclipseSonar index = EclipseSonar.getInstance(project);
        SourceCode sourceCode = index.search(project);
        if (sourceCode != null) {
          final Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(sourceCode.getKey(), ProfileUtil.METRIC_KEY));
          final Measure measure = resource.getMeasure(ProfileUtil.METRIC_KEY);
          if ( !monitor.isCanceled() && measure != null) {
            List<Rule> rules = sourceCode.getRules();
            if ( !monitor.isCanceled() && rules != null) {
              convertProfile(measure.getData(), project, rules, monitor);
            }
          }
        }
      }
    }
    return Status.OK_STATUS;
  }

  private void convertProfile(String profileName, IProject project, List<Rule> rules, IProgressMonitor monitor) {
    ProfileConfiguration configuration = new ProfileConfiguration(profileName, project);
    List<ISonarRuleConverter> converters = getConverters();
    for (Rule rule : rules) {
      for (ISonarRuleConverter converter : converters) {
        if (converter.canConvert(rule))
          converter.convert(configuration, rule);
      }
    }
    configuration.apply();
  }

  private List<ISonarRuleConverter> getConverters() {
    final List<ISonarRuleConverter> converters = new ArrayList<ISonarRuleConverter>();
    final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(CONVERTER_ID);
    try {
      for (final IConfigurationElement element : config) {
        final Object converter = element.createExecutableExtension("class");
        if (converter instanceof ISonarRuleConverter) {
          converters.add((ISonarRuleConverter) converter);
        }
      }
    } catch (final CoreException ex) {
      SonarUiPlugin.getDefault().displayError(IStatus.WARNING, "Error in RetrieveSonarProfileJob.", ex, true);
    }
    return converters;
  }
}
