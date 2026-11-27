package devoxx;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class NullPointerDereferenceTest {

  private NullPointerDereference underTest = new NullPointerDereference();

  @Test
  @Ignore
  public void shouldGetDogPaws() {
    assertEquals(underTest.getNumberOfPawsPlusOne("dog"), 5);
  }

  @Test(expected = NullPointerException.class)
  @Ignore
  public void ignoredTestThatWouldCoverBug() {
    underTest.getNumberOfPawsPlusOne("");
  }
}
