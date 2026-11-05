package hello;

public class MultiFlows {
  public void multi(int x) {
    boolean flag;
    if (x > 0) {
      flag = true;
      System.out.println("positive");
    } else {
      flag = true;
      System.out.println("not positive");
    }
    if (flag) {
      System.out.println("flag is always true here");
    }
  }
}
