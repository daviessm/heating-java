package uk.me.steev.java.heating.io.boiler;

import uk.me.steev.java.heating.io.boiler.usb.UsbUtils;

public class Boiler {
  protected Relay heatingRelay;
  protected Relay preheatRelay;
  
  public Boiler(Relay heatingRelay, Relay preheatRelay) {
    this.heatingRelay = heatingRelay;
    this.preheatRelay = preheatRelay;
  }
  
  public void startHeating() {
    this.heatingRelay.on();
  }
  
  public void stopHeating() {
    this.heatingRelay.off();
  }
  
  public void startPreheating() {
    this.preheatRelay.on();
  }
  
  public void stopPreheating() {
    this.preheatRelay.off();
  }
  
  public boolean isHeating() {
    return heatingRelay.isOn();
  }
  
  public boolean isPreheating() {
    return preheatRelay.isOn();
  }

  public static void main (String[] args) {
    UsbUtils.deinit();
    Relay heatingRelay = Relay.findRelay(RelayTypes.USB_1, new String[]{"3","14"});
    Relay preheatRelay = Relay.findRelay(RelayTypes.USB_1, new String[]{"3","14"});

    Boiler b = new Boiler(heatingRelay, preheatRelay);
    try{
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
    UsbUtils.deinit();
  }
}
