/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarClient;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.ui.Messages;
import org.sonar.ide.eclipse.internal.mylyn.ui.SonarMylynUiPlugin;

import java.net.MalformedURLException;
import java.net.URL;

public class SonarRepositorySettingsPage extends AbstractRepositorySettingsPage {

  public SonarRepositorySettingsPage(TaskRepository taskRepository) {
    super(Messages.SonarRepositorySettingsPage_Title, Messages.SonarRepositorySettingsPage_Description, taskRepository);
    setNeedsAnonymousLogin(false);
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
      } catch (MalformedURLException e) { // NOSONAR: ignore
      }
    }
    return false;
  }

  @Override
  protected Validator getValidator(TaskRepository repository) {
    return new SonarValidator(repository);
  }

  public class SonarValidator extends Validator {
    private final TaskRepository repository;

    public SonarValidator(TaskRepository repository) {
      this.repository = repository;
    }

    @Override
    public void run(IProgressMonitor monitor) throws CoreException {
      try {
        String version = new SonarClient(repository).getServerVersion();
        if (!SonarConnector.isServerVersionSupported(version)) {
          setStatus(RepositoryStatus.createStatus(repository, IStatus.ERROR, SonarMylynUiPlugin.PLUGIN_ID,
              NLS.bind(Messages.SonarRepositorySettingsPage_Unsupported_version, version, SonarConnector.MINIMAL_VERSION)));
        }
      } catch (Exception e) {
        setStatus(RepositoryStatus.createStatus(repository, IStatus.ERROR, SonarMylynUiPlugin.PLUGIN_ID,
            Messages.SonarRepositorySettingsPage_Connection_failed));
      }
    }
  }

  /**
   * Value of org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants#PROPERTY_CATEGORY , which is not available in Mylyn 3.2.0
   */
  private static final String PROPERTY_CATEGORY = "category"; //$NON-NLS-1$

  /**
   * Value of org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants#CATEGORY_REVIEW , which is not available in Mylyn 3.2.0
   */
  private static final String CATEGORY_REVIEW = "org.eclipse.mylyn.category.review"; //$NON-NLS-1$

  @Override
  public void applyTo(TaskRepository repository) {
    super.applyTo(repository);
    repository.setProperty(PROPERTY_CATEGORY, CATEGORY_REVIEW);
  }
}
