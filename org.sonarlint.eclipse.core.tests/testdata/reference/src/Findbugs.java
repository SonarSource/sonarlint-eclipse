public final class Findbugs {
  private int x = 1;

  public int test() {
    this.x = x; // FindBugs violation - Self assignment of field
    return x;
  }
}
