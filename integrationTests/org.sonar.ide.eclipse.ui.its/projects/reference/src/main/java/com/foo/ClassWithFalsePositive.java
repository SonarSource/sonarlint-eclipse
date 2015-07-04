package com.foo;

public class ClassWithFalsePositive {

  private String unusedMethod() { // Issue here - will be flagged FP
    return "hello";
  }

}
