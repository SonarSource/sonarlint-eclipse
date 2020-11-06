package com.sonarsource;

public class NpeWithFlow2 {

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