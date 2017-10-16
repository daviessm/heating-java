package uk.me.steev.java.heating.io.boiler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Boiler {
  static final Logger logger = LogManager.getLogger(Boiler.class.getName());
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
}
