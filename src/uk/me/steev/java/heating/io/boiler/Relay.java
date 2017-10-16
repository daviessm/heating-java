package uk.me.steev.java.heating.io.boiler;

import uk.me.steev.java.heating.io.boiler.usb.UsbRelay;

public abstract class Relay {
  public static Relay findRelay(RelayTypes type, String[] address) throws RelayException {
    switch (type) {
    case USB_1:
      return UsbRelay.getRelay(address);
    }
    return null;
  }
  
  public abstract void on() throws RelayException;
  public abstract void off() throws RelayException;
  
  public abstract boolean isOn() throws RelayException;
}
