package uk.me.steev.java.heating.io.boiler.usb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.io.boiler.Relay;

public class UsbRelay extends Relay {
  static final Logger logger = LogManager.getLogger(UsbRelay.class.getName());
  protected UsbDevice device;
  protected boolean on = false;
  protected byte relayNumber = 0;

  public static Relay getRelay(String[] address) {
    if (address.length < 3)
      return null;

    byte relayNumber = Byte.parseByte(address[0]);
    String[] usbAddress = Arrays.copyOfRange(address, 1, address.length);
    Map<UsbPhysicalLocation, UsbDevice> relays = findRelays();
    for (UsbPhysicalLocation physicalLocation : relays.keySet()) {
      if (physicalLocation.getBusNumber() == Integer.parseInt(usbAddress[0])) {
        byte[] locationOnBus = physicalLocation.getLocationOnBus();
        if (locationOnBus.length != usbAddress.length - 1)
          continue;

        int i = 1;
        boolean match = true;
        for (byte b : locationOnBus) {
          if (b != Byte.parseByte(usbAddress[i++])) {
            match = false;
            break;
          }
        }
        if (match)
          return new UsbRelay(relays.get(physicalLocation), relayNumber);
      }
    }
    return null;
  }

  protected static Map<UsbPhysicalLocation, UsbDevice> findRelays() {
    Map<UsbPhysicalLocation, UsbDevice> relays = new HashMap<>();
    List<UsbDevice> devices = null;

    try {
      devices = UsbUtils.findDevices("16C0", "05DF");
      for (UsbDevice device : devices) {
        relays.put(device.getPhysicalLocation(), device);
      }
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }
    return relays;
  }

  protected UsbRelay(UsbDevice device, byte relayNumber) {
    this.relayNumber = relayNumber;
    this.device = device;
    this.off();
  }

  public void on() {
    logger.trace("Relay ON: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFF, (byte)this.relayNumber, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }
    this.on = true;
  }

  public void off() {
    logger.trace("Relay OFF: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFD, (byte)this.relayNumber, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }
    this.on = false;
  }

  public boolean isOn() {
    return on;
  }

  public String toString() {
    return device.getPhysicalLocation().toString();
  }
}
