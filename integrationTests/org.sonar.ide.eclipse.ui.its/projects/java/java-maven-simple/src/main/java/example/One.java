package example;

public class One {
  String message = "foo";

  public String foo() {
    return message;
  }

  public void uncoveredMethod() {
    System.out.println(foo());
    System.out.println("hhh");
    System.out.println("hhh");
	    System.out.println("hhh");
    System.out.println("hhh");

  }
}
