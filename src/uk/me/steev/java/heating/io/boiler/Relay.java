package uk.me.steev.java.heating.io.boiler;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.HeatingConfiguration.RelayConfiguration;
import uk.me.steev.java.heating.io.boiler.usb.PhysicalRelay;
import uk.me.steev.java.heating.io.boiler.usb.UsbRelayBoard;

public abstract class Relay {
  private static final Logger logger = LogManager.getLogger(Relay.class.getName());
  private RelayType relayType;
  private ArrayList<String> address;
  private int relayNumber;

  public static Relay findRelay(RelayType relayType, RelayConfiguration relayConfiguration) {
    Relay relay = null;
    switch (relayType) {
    case USB:
      UsbRelayBoard board = UsbRelayBoard.getRelay(relayConfiguration.getAddress());
      logger.info("Found relay " + board.toString() + " " + relayConfiguration.getRelayNumber());
      relay = new PhysicalRelay(board, relayConfiguration.getRelayNumber());
    }

    return relay;
  }

  public abstract void on() throws RelayException;
  public abstract void off() throws RelayException;

  public abstract boolean isOn() throws RelayException;

  public RelayType getRelayType() {
    return relayType;
  }
  public void setRelayType(RelayType relayType) {
    this.relayType = relayType;
  }
  public ArrayList<String> getAddress() {
    return address;
  }

  public void setAddress(ArrayList<String> address) {
    this.address = address;
  }

  public int getRelayNumber() {
    return relayNumber;
  }

  public void setRelayNumber(int relayNumber) {
    this.relayNumber = relayNumber;
  }
}
