package uk.me.steev.java.heating.io.boiler;

import uk.me.steev.java.heating.io.boiler.usb.UsbRelay;

public abstract class Relay {
  public static Relay findRelay(String[] address) throws RelayException {
    return UsbRelay.getRelay(address);
  }

  public abstract void on() throws RelayException;
  public abstract void off() throws RelayException;

  public abstract boolean isOn() throws RelayException;
}
