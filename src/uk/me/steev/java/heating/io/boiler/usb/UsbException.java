package uk.me.steev.java.heating.io.boiler.usb;

import uk.me.steev.java.heating.io.boiler.RelayException;

public class UsbException extends RelayException {
  private static final long serialVersionUID = 1113634216294921221L;

  public UsbException(String message) {
    super(message);
  }
  
  public UsbException(String message, Throwable exception) {
    super(message, exception);
  }
}
