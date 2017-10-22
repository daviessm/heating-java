package uk.me.steev.java.heating.io.boiler;

import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Boiler {
  static final Logger logger = LogManager.getLogger(Boiler.class.getName());
  protected Relay heatingRelay;
  protected Relay preheatRelay;
  protected LocalDateTime timeHeatingOn;
  protected LocalDateTime timeHeatingOff;

  public Boiler(Relay heatingRelay, Relay preheatRelay) {
    this.heatingRelay = heatingRelay;
    this.preheatRelay = preheatRelay;
    this.timeHeatingOff = LocalDateTime.now().minusDays(1);
  }

  public void startHeating() throws RelayException {
    logger.info("Heating on");
    this.heatingRelay.on();
    this.timeHeatingOff = null;
    this.timeHeatingOn = LocalDateTime.now();
  }

  public void stopHeating() throws RelayException {
    logger.info("Heating off");
    this.heatingRelay.off();
    this.timeHeatingOn = null;
    this.timeHeatingOff = LocalDateTime.now();
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

  public LocalDateTime getTimeHeatingOn() {
    return timeHeatingOn;
  }

  public void setTimeHeatingOn(LocalDateTime timeHeatingOn) {
    this.timeHeatingOn = timeHeatingOn;
  }

  public LocalDateTime getTimeHeatingOff() {
    return timeHeatingOff;
  }

  public void setTimeHeatingOff(LocalDateTime timeHeatingOff) {
    this.timeHeatingOff = timeHeatingOff;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Boiler [heatingRelay=").append(heatingRelay).append(", preheatRelay=").append(preheatRelay)
        .append(", heatingOn=").append(timeHeatingOn).append(", heatingOff=").append(timeHeatingOff)
        .append(", proportionalTime=").append("]");
    return builder.toString();
  }
}
