package uk.me.steev.java.heating.io.boiler.usb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.io.boiler.Relay;

public class PhysicalRelay extends Relay {
  private static final Logger logger = LogManager.getLogger(PhysicalRelay.class.getName());
  protected UsbRelayBoard board;
  protected boolean on = false;
  protected int relayNumber = 0;

  public PhysicalRelay(UsbRelayBoard board, int relayNumber) {
    this.relayNumber = relayNumber;
    this.board = board;
    this.off();
  }

  public void on() {
    logger.trace("Relay ON: " + this.toString());
    this.board.on(this.relayNumber);
    this.on = true;
  }

  public void off() {
    logger.trace("Relay OFF: " + this.toString());
    this.board.off(this.relayNumber);
    this.on = false;
  }

  public boolean isOn() {
    return on;
  }

  public String toString() {
    return board.getDevice().getPhysicalLocation().toString() + " relayNumber " + this.relayNumber;
  }
}
