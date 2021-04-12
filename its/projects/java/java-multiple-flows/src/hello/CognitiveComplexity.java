package hello;

import java.util.logging.Logger;

public class CognitiveComplexity {

  private static final Logger LOG = Logger.getGlobal();

  private CognitiveComplexity() {
    // Nothing
  }

  public static void main(String... args) {
    String result = Integer.toString(cognitivelyComplex(1, 0, 1, 0));
    LOG.warning(result);
  }

  public static int cognitivelyComplex(int a, int b, int c, int d) {
    if (a < b) {
      if (a < c) {
        if (a < d) {
          return a;
        } else if (b < c) {
          return b;
        }
      } else {
        return b;
      }
    } else if (a + d < b) {
      if (a < c) {
        if (a < d) {
          return a;
        } else if (a + b < d) {
          return b;
        }
      } else {
        return b;
      }
    } else {
      if (a < c) {
        if (a < d) {
          return a;
        } else if (a + b < d) {
          return b;
        }
      } else {
        return b;
      }
    }
    return -1;
  }
}
