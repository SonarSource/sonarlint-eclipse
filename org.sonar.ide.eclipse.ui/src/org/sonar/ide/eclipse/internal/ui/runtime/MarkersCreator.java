/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.ui.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.EmbedderIndex;
import org.sonar.ide.eclipse.core.ResourceResolver;
import org.sonar.ide.eclipse.core.SonarCorePlugin;

public class MarkersCreator implements IResourceVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(MarkersCreator.class);

  private List<ResourceResolver> resolvers;

  private IProgressMonitor monitor;

  private EmbedderIndex index;

  public MarkersCreator(IProgressMonitor monitor, EmbedderIndex index) {
    this.monitor = monitor;
    this.index = index;
  }

  private String resolve(IResource resource) {
    if (resolvers == null) {
      resolvers = new ArrayList<ResourceResolver>();
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IConfigurationElement[] config = registry.getConfigurationElementsFor("org.sonar.ide.eclipse.core.resourceResolvers"); //$NON-NLS-1$
      for (final IConfigurationElement element : config) {
        try {
          Object obj = element.createExecutableExtension(ResourceResolver.ATTR_CLASS);
          resolvers.add((ResourceResolver) obj);
        } catch (CoreException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }

    for (ResourceResolver resolver : resolvers) {
      String sonarKey = resolver.resolve(resource, monitor);
      if (sonarKey != null) {
        return sonarKey;
      }
    }
    return null;
  }

  public boolean visit(IResource resource) throws CoreException {
    resource.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_ZERO);
    if (resource instanceof IFile) {
      String sonarKey = resolve(resource);
      if (sonarKey != null) {
        Collection<Violation> violations = index.getViolations(sonarKey);
        if (violations != null) {
          for (Violation violation : violations) {
            // TODO move to helper class
            final IMarker marker = resource.createMarker(SonarCorePlugin.MARKER_ID);
            final Map<String, Object> markerAttributes = new HashMap<String, Object>();
            markerAttributes.put(IMarker.LINE_NUMBER, violation.getLineId());
            markerAttributes.put(IMarker.MESSAGE, violation.getMessage());
            markerAttributes.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
            markerAttributes.put(IMarker.PRIORITY, IMarker.PRIORITY_LOW);

            Rule rule = violation.getRule();
            markerAttributes.put("rulekey", rule.getKey()); //$NON-NLS-1$
            markerAttributes.put("rulename", rule.getName()); //$NON-NLS-1$
            markerAttributes.put("rulepriority", rule.getSeverity().toString()); //$NON-NLS-1$
            marker.setAttributes(markerAttributes);
          }
        }
      }
      return false; // don't go deeper than file
    }
    // TODO handle violations not only on files
    return true;
  }

}
