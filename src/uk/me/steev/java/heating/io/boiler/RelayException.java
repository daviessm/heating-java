package uk.me.steev.java.heating.io.boiler;

public class RelayException extends Exception {
  private static final long serialVersionUID = -5686506645517580823L;

  public RelayException(String message) {
    super(message);
  }
  
  public RelayException(String message, Throwable exception) {
    super(message, exception);
  }
}
