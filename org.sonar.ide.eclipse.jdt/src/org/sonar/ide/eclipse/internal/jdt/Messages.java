package org.sonar.ide.eclipse.internal.jdt;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.internal.jdt.messages"; //$NON-NLS-1$

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  public static String NoSonarResolver_label;
  public static String NoSonarResolver_description;

}
