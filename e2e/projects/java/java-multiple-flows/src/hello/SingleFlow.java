package hello;

public class SingleFlow {
  public void single(boolean a) {
    boolean b = a;
    if (a) {
      if (b) {
        System.out.println("b is always true here");
      }
    }
  }
}
