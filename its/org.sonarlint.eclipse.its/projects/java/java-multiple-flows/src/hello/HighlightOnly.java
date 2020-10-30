package hello;

import java.util.logging.Logger;

public class HighlightOnly {

  private static final Logger LOG = Logger.getGlobal();

  private HighlightOnly() {
    // Nothing
  }

  public static void main(String[] args) {
    if (((args.length > 1))) {
      for (String arg: args) {
        LOG.warning(arg);
      }
    }
  }
}
