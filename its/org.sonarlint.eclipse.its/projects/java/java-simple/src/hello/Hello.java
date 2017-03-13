package hello;

import java.util.Arrays;
import java.util.function.Function;
import junit.framework.Test;

public class Hello {

  private Hello() {
  }

  public static void main(String... args) {
    System.out.println("Hello");

    Arrays.asList("foo", "bar").stream().map(new Function<String, String>() {
      public String apply(String input) {
        return new StringBuilder(input).reverse().toString();
      }
    });

  }

  public int unecessaryCast(Test test) {
    return (int) test.countTestCases();
  }

}
