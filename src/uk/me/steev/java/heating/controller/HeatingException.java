package uk.me.steev.java.heating.controller;

public class HeatingException extends Exception {
  private static final long serialVersionUID = 4859014937850098130L;

  public HeatingException(String message) {
    super(message);
  }
  
  public HeatingException(String message, Throwable exception) {
    super(message, exception);
  }
}
