package findbugs;

import deps.FindbugsUtils;

public final class Findbugs {

  public int test() {
    int x = 1;
    x = x; // Issue - Self assignment of variable
    FindbugsUtils utils = new FindbugsUtils();
    utils.test();
    return x;
  }
}
