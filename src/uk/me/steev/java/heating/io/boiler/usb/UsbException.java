package uk.me.steev.java.heating.io.boiler.usb;

public class UsbException extends Exception {
  private static final long serialVersionUID = 1113634216294921221L;

  public UsbException(String message) {
    super(message);
  }
  
  public UsbException(String message, Throwable exception) {
    super(message, exception);
  }
}
