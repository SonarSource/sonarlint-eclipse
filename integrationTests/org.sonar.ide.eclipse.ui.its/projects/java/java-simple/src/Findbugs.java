public final class Findbugs {

  public int test() {
    int x = 1;
    x = x; // Self assignment
    return x;
  }
}
