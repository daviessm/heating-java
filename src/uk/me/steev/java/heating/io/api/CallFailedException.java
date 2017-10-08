package uk.me.steev.java.heating.io.api;

public class CallFailedException extends Exception {
  private static final long serialVersionUID = -6552815615691968638L;

  public CallFailedException(String message) {
    super(message);
  }
  
  public CallFailedException(String message, Throwable exception) {
    super(message, exception);
  }
}
