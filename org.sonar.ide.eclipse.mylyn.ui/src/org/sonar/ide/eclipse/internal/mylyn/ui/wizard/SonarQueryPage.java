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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonar.ide.eclipse.internal.mylyn.ui.Messages;

public class SonarQueryPage extends AbstractRepositoryQueryPage {

  private IRepositoryQuery query;
  private Text titleText;

  public SonarQueryPage(TaskRepository repository, IRepositoryQuery query) {
    super(Messages.SonarQueryPage_Title, repository, query);
    setTitle(Messages.SonarQueryPage_Title);
    setDescription(Messages.SonarQueryPage_Description);
    this.query = query;
  }

  public void createControl(Composite parent) {
    Composite control = new Composite(parent, SWT.NONE);
    control.setLayoutData(new GridData(GridData.FILL_BOTH));
    control.setLayout(new GridLayout(3, false));

    KeyListener keyListener = new KeyListener() {
      public void keyPressed(KeyEvent e) {
        // ignore
      }

      public void keyReleased(KeyEvent e) {
        getContainer().updateButtons();
      }
    };

    Label titleLabel = new Label(control, SWT.NONE);
    titleLabel.setText(Messages.SonarQueryPage_Query_Title);
    titleText = new Text(control, SWT.BORDER);
    titleText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    titleText.addKeyListener(keyListener);

    if (query != null) {
      titleText.setText(query.getSummary());
    }

    Dialog.applyDialogFont(control);
    setControl(control);
  }

  @Override
  public String getQueryTitle() {
    return titleText.getText();
  }

  @Override
  public void applyTo(IRepositoryQuery query) {
    query.setSummary(getQueryTitle());
  }

}
