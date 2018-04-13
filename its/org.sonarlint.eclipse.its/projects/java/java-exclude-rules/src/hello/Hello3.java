package hello;

public class Hello3 {

  private Hello3() {
  }

  public static void main(String... args) {
    System.out.println("Hello");
  }

  public int cognitivelyComplex(int a, int b, int c, int d) {
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
