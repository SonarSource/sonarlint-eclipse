package hello;

import java.util.Arrays;
import java.util.function.Function;

public class Hello {

  private Hello() {
  }

  public static void main(String... args) {

    Arrays.asList("foo", "bar").stream().map(new Function<String, String>() {
      @Override
      public String apply(String input) {
        return new StringBuilder(input).reverse().toString();
      }
    });

  }

}
