package org.sonar.ide.eclipse.internal.jdt.profiles.checkstyle;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.sonar.ide.eclipse.internal.jdt.profiles.ProfileConfiguration;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 * 
 *        Class to convert Checkstyle LineLengthCheck rule to eclipse profile.
 * 
 *        ex : {"title":"Unused Imports","key":"checkstyle:com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck","plugin":
 *        "checkstyle","description":"Checks for unused import statements.","priority":"INFO","status":"ACTIVE"},
 */
@SuppressWarnings("restriction")
public class UnusedImportsCheckstyleConverter extends AbstractCheckstyleConverter {

  protected final static String KEY = "checkstyle:com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck";

  public UnusedImportsCheckstyleConverter() {
    super(KEY);
  }
  public void convert(ProfileConfiguration config, Rule rule) {
    config.addFormat(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS,CleanUpOptions.TRUE);
  }

}
