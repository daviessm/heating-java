package uk.me.steev.java.heating.io.boiler;

import uk.me.steev.java.heating.io.boiler.usb.UsbUtils;

public class Boiler {
  protected Relay heatingRelay;
  protected Relay preheatRelay;

  public Boiler(Relay heatingRelay, Relay preheatRelay) {
    this.heatingRelay = heatingRelay;
    this.preheatRelay = preheatRelay;
  }

  public void startHeating() throws RelayException {
    this.heatingRelay.on();
  }

  public void stopHeating() throws RelayException {
    this.heatingRelay.off();
  }

  public void startPreheating() throws RelayException {
    this.preheatRelay.on();
  }

  public void stopPreheating() throws RelayException {
    this.preheatRelay.off();
  }

  public boolean isHeating() throws RelayException {
    return heatingRelay.isOn();
  }

  public boolean isPreheating() throws RelayException {
    return preheatRelay.isOn();
  }

  public static void main(String[] args) {
    UsbUtils.deinit();
    Relay heatingRelay = null;
    Relay preheatRelay = null;
    try {
      heatingRelay = Relay.findRelay(RelayTypes.USB_1, new String[] { "3", "14" });
      preheatRelay = Relay.findRelay(RelayTypes.USB_1, new String[] { "3", "14" });
      Boiler b = new Boiler(heatingRelay, preheatRelay);
      try {
        b.startHeating();
        Thread.sleep(1000);
        b.startHeating();
        Thread.sleep(1000);
        b.stopHeating();
        Thread.sleep(1000);
        b.stopHeating();
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      }
    } catch (RelayException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    UsbUtils.deinit();
  }
}
