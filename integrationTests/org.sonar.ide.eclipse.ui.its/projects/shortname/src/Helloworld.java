public class Helloworld {

  private String field;

  public void use() {
    new DeprecatedExample().deprecatedMethod();
    System.exit(33); // violation on findbugs rule: DM_EXIT
  }

  public void useFieldForLcom4() {
    System.out.println(field);
  }
}
