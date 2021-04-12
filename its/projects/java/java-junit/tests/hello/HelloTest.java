package hello;

import org.junit.Ignore;
import org.junit.Test;

public class HelloTest {

  @Test
  @Ignore
  public void testHello() throws InterruptedException {
    Hello.main();
  }

}
