package org.sonarlint.eclipse.its.reddeer.preferences;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;

public class SonarLintPreferences extends PropertyPage {

  public static final String NAME = "SonarLint";

  public SonarLintPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, NAME);
  }

}
