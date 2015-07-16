package deps;

// This class is used by Findbugs.classbut should not produce violations
public final class FindbugsUtils {
  
  public String returnNull = null;
  private int x = 1;

  public int test() {
    x = x; // FindBugs violation - Self assignment of field
    return x;
  }
  
  public String returnNull() {
	return null;
  }
}
