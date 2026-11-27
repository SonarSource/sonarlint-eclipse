package hello;

public class Hello {

  private Hello() {
  }

  public int unnecessaryCast() {
    return (int) hashCode();
  }

}
