package uk.me.steev.java.heating.io.boiler;

import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Boiler {
  static final Logger logger = LogManager.getLogger(Boiler.class.getName());
  protected Relay heatingRelay;
  protected Relay preheatRelay;
  protected LocalDateTime heatingOn;
  protected LocalDateTime heatingOff;

  public Boiler(Relay heatingRelay, Relay preheatRelay) {
    this.heatingRelay = heatingRelay;
    this.preheatRelay = preheatRelay;
  }

  public void startHeating() throws RelayException {
    logger.info("Heating on");
    this.heatingRelay.on();
    this.heatingOff = null;
    this.heatingOn = LocalDateTime.now();
  }

  public void stopHeating() throws RelayException {
    logger.info("Heating off");
    this.heatingRelay.off();
    this.heatingOn = null;
    this.heatingOff = LocalDateTime.now();
  }

  public void startPreheating() throws RelayException {
    logger.info("Preheating on");
    this.preheatRelay.on();
  }

  public void stopPreheating() throws RelayException {
    logger.info("Preheating off");
    this.preheatRelay.off();
  }

  public boolean isHeating() throws RelayException {
    return heatingRelay.isOn();
  }

  public boolean isPreheating() throws RelayException {
    return preheatRelay.isOn();
  }

  public Relay getHeatingRelay() {
    return heatingRelay;
  }

  public void setHeatingRelay(Relay heatingRelay) {
    this.heatingRelay = heatingRelay;
  }

  public Relay getPreheatRelay() {
    return preheatRelay;
  }

  public void setPreheatRelay(Relay preheatRelay) {
    this.preheatRelay = preheatRelay;
  }

  public LocalDateTime getHeatingOn() {
    return heatingOn;
  }

  public void setHeatingOn(LocalDateTime heatingOn) {
    this.heatingOn = heatingOn;
  }

  public LocalDateTime getHeatingOff() {
    return heatingOff;
  }

  public void setHeatingOff(LocalDateTime heatingOff) {
    this.heatingOff = heatingOff;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Boiler [heatingRelay=").append(heatingRelay).append(", preheatRelay=").append(preheatRelay)
        .append(", heatingOn=").append(heatingOn).append(", heatingOff=").append(heatingOff).append("]");
    return builder.toString();
  }
}
