public final class Findbugs2 {

  public int test() {
    int x = 1;
    x = x; // Issue - Self assignment of field
    return x;
  }
}
