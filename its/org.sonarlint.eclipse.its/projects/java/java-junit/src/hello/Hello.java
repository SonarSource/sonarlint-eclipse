package hello;

import junit.framework.Test;

public class Hello {

  private Hello() {
  }

  public static void main(String... args) {
    System.out.println("Hello");
  }

  public int unecessaryCast(Test test) {
    return (int) test.countTestCases();
  }

}
