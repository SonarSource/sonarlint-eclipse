import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PmdTest {

  @Test
  public void myTestMethod() {
    int i = 0;
    int j = i++; // No violation because we are in a test
    List r = new ArrayList();
    r.add("toto");
    assertTrue("not empty", !r.isEmpty()); // Violation
  }
}
