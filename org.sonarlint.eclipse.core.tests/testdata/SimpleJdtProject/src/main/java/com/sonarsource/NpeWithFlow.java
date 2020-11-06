package com.sonarsource;

public class NpeWithFlow {

  public int foo(boolean a, String foo) {
    if (a) {
      foo = null;
    } else {
      foo = null;
    }
    foo.toString();
    return 0;
  }
}