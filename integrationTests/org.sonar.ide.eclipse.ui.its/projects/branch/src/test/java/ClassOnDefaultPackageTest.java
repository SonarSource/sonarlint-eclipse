import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClassOnDefaultPackageTest {

  @Test
  public void myTestMethod() {
    int i = 0;
    int j = i++;
    List r = new ArrayList();
    r.add("toto");
    assertTrue("not empty", !r.isEmpty()); // Issue: simplify boolean assertions
  }
}
