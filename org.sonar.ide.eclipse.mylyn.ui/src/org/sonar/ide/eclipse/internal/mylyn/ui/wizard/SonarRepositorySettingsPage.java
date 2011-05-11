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
package org.sonar.ide.eclipse.internal.mylyn.ui.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.ui.Messages;

import java.net.MalformedURLException;
import java.net.URL;

public class SonarRepositorySettingsPage extends AbstractRepositorySettingsPage {

  public SonarRepositorySettingsPage(TaskRepository taskRepository) {
    super(Messages.SonarRepositorySettingsPage_Title, Messages.SonarRepositorySettingsPage_Description, taskRepository);
    setNeedsAnonymousLogin(true);
    setNeedsValidation(true);
    setNeedsAdvanced(false);
    setNeedsEncoding(false);
    setNeedsTimeZone(false);
    setNeedsHttpAuth(false);
    setNeedsProxy(false); // We use settings from IDE
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    addRepositoryTemplatesToServerUrlCombo();
  }

  @Override
  public String getConnectorKind() {
    return SonarConnector.CONNECTOR_KIND;
  }

  @Override
  protected void createAdditionalControls(Composite parent) {
    // ignore, advanced section is disabled
  }

  @Override
  protected void repositoryTemplateSelected(RepositoryTemplate template) {
    repositoryLabelEditor.setStringValue(template.label);
    setUrl(template.repositoryUrl);
    setAnonymous(template.anonymous);

    getContainer().updateButtons();
  }

  /**
   * {@inheritDoc} Visibility has been relaxed for test.
   */
  @Override
  public boolean isValidUrl(String url) {
    if ((url.startsWith(URL_PREFIX_HTTPS) || url.startsWith(URL_PREFIX_HTTP)) && !url.endsWith("/")) { //$NON-NLS-1$
      try {
        new URL(url);
        return true;
      } catch (MalformedURLException e) {
      }
    }
    return false;
  }

  @Override
  protected Validator getValidator(TaskRepository repository) {
    return new SonarValidator();
  }

  public class SonarValidator extends Validator {
    @Override
    public void run(IProgressMonitor monitor) throws CoreException {
      // TODO validate server version
    }
  }

  @SuppressWarnings("restriction")
  @Override
  public void applyTo(TaskRepository repository) {
    super.applyTo(repository);
    repository.setProperty(IRepositoryConstants.PROPERTY_CATEGORY, IRepositoryConstants.CATEGORY_REVIEW);
  }
}
