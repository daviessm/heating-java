package uk.me.steev.java.heating.io.boiler;

public abstract class Relay {
  public static Relay findRelay(RelayTypes type, String[] address) {
    switch (type) {
    case USB_1:
      return UsbRelay.getRelay(address);
    }
    return null;
  }
  
  public abstract void on();
  public abstract void off();
  
  public abstract boolean isOn();
}
