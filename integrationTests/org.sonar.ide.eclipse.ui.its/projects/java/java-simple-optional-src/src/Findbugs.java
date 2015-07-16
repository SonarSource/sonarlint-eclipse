public final class Findbugs {

  public int test() {
    int x = 1;
    x = x; // Issue - Self assignment of field
    return x;
  }
}
