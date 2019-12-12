package uk.me.steev.java.heating.io.boiler;

import java.util.ArrayList;

import uk.me.steev.java.heating.controller.HeatingConfiguration.RelayConfiguration;
import uk.me.steev.java.heating.io.boiler.usb.PhysicalRelay;
import uk.me.steev.java.heating.io.boiler.usb.UsbRelayBoard;

public class Relay {
  private RelayType relayType;
  private ArrayList<String> address;
  private int relayNumber;

  public static Relay findRelay(RelayType relayType, RelayConfiguration relayConfiguration) {
    Relay relay = null;
    switch (relayType) {
    case USB:
      UsbRelayBoard board = UsbRelayBoard.getRelay(relayConfiguration.getAddress());
      relay = new PhysicalRelay(board, relayConfiguration.getRelayNumber());
    }

    return relay;
  }

  public void on() throws RelayException {}
  public void off() throws RelayException {}

  public boolean isOn() throws RelayException {
    return false;
  }

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
