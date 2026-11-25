/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.SonarCloudRegion;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionTypeWizardPageTest {
  private Shell parentShell;
  private WizardDialog dialog;

  @Before
  public void setUp() {
    parentShell = new Shell(Display.getDefault());
  }

  @After
  public void tearDown() {
    if (dialog != null) {
      dialog.close();
    }
    parentShell.dispose();
  }

  @Test
  public void offers_one_radio_per_region_without_invite_only_label() {
    createPage(new ServerConnectionModel());

    assertThat(findRadio("EU - sonarcloud.io")).isNotNull();
    assertThat(findRadio("US - sonarqube.us")).isNotNull();
    assertThat(findRadio("AU - sonarqube.au")).isNotNull();
    assertThat(radios()).extracting(Button::getText).noneMatch(text -> text.contains("invite-only"));
  }

  @Test
  public void selecting_a_region_updates_the_model() {
    var model = new ServerConnectionModel();
    createPage(model);

    select(findRadio("AU - sonarqube.au"));
    assertThat(model.getSonarCloudRegion()).isEqualTo(SonarCloudRegion.AU);
    assertThat(model.getServerUrl()).isEqualTo("https://sonarqube.au");

    select(findRadio("US - sonarqube.us"));
    assertThat(model.getSonarCloudRegion()).isEqualTo(SonarCloudRegion.US);

    select(findRadio("EU - sonarcloud.io"));
    assertThat(model.getSonarCloudRegion()).isEqualTo(SonarCloudRegion.EU);
  }

  @Test
  public void region_preset_in_the_model_selects_its_radio() {
    var model = new ServerConnectionModel();
    model.setSonarCloudRegion(SonarCloudRegion.AU);
    createPage(model);

    assertThat(findRadio("AU - sonarqube.au").getSelection()).isTrue();
    assertThat(findRadio("EU - sonarcloud.io").getSelection()).isFalse();
    assertThat(findRadio("US - sonarqube.us").getSelection()).isFalse();
  }

  @Test
  public void region_radios_are_disabled_for_sonarqube_server_only() {
    var model = new ServerConnectionModel();
    createPage(model);
    var regionRadios = List.of(findRadio("EU - sonarcloud.io"), findRadio("US - sonarqube.us"), findRadio("AU - sonarqube.au"));

    select(connectionTypeRadios().get(1));
    assertThat(model.getConnectionType()).isEqualTo(ConnectionType.ONPREMISE);
    assertThat(regionRadios).noneMatch(Button::getEnabled);

    select(connectionTypeRadios().get(0));
    assertThat(regionRadios).allMatch(Button::getEnabled);
  }

  private void createPage(ServerConnectionModel model) {
    var page = new ConnectionTypeWizardPage(model);
    var wizard = new Wizard() {
      @Override
      public void addPages() {
        addPage(page);
      }

      @Override
      public boolean performFinish() {
        return true;
      }
    };
    dialog = new WizardDialog(parentShell, wizard);
    dialog.setBlockOnOpen(false);
    Realm.runWithDefault(DisplayRealm.getRealm(parentShell.getDisplay()), dialog::create);
  }

  private static void select(Button radio) {
    // Programmatic selection does not clear the sibling radios or notify them like a user click does
    for (var sibling : radio.getParent().getChildren()) {
      if (sibling instanceof Button && sibling != radio && ((Button) sibling).getSelection()) {
        ((Button) sibling).setSelection(false);
        sibling.notifyListeners(SWT.Selection, null);
      }
    }
    radio.setSelection(true);
    radio.notifyListeners(SWT.Selection, null);
  }

  private Button findRadio(String text) {
    return radios().stream().filter(b -> text.equals(b.getText())).findFirst().orElse(null);
  }

  private List<Button> connectionTypeRadios() {
    return radios().stream().filter(b -> b.getText().isEmpty()).collect(Collectors.toList());
  }

  private List<Button> radios() {
    var result = new ArrayList<Button>();
    collectRadios(dialog.getShell(), result);
    return result;
  }

  private static void collectRadios(Composite parent, List<Button> result) {
    for (Control child : parent.getChildren()) {
      if (child instanceof Button && (child.getStyle() & SWT.RADIO) != 0) {
        result.add((Button) child);
      } else if (child instanceof Composite) {
        collectRadios((Composite) child, result);
      }
    }
  }
}
