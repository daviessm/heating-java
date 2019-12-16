package uk.me.steev.java.heating.io.boiler.usb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UsbRelayBoard implements Comparable<UsbRelayBoard> {
  private static final Logger logger = LogManager.getLogger(UsbRelayBoard.class.getName());
  private UsbDevice device;

  public static UsbRelayBoard getRelay(ArrayList<String> address) {
    if (address.size() < 2) //bus number, device number [, device number...]
      return null;

    //Set up list of all devices
    Map<UsbPhysicalLocation, UsbDevice> allDevices = new TreeMap<UsbPhysicalLocation, UsbDevice>();
    logger.trace("Looking for USB devices");
    List<UsbDevice> devices = null;

    try {
      devices = UsbUtils.findDevices("16C0", "05DF");
      for (UsbDevice device : devices) {
        allDevices.put(device.getPhysicalLocation(), device);
        logger.debug("Found device at " + device.getPhysicalLocation());
      }
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }

    for (UsbPhysicalLocation physicalLocation : allDevices.keySet()) {
      if (physicalLocation.getBusNumber() == Integer.parseInt(address.get(0))) {
        byte[] locationOnBus = physicalLocation.getLocationOnBus();
        if (locationOnBus.length != address.size() - 1)
          continue;

        int i = 1;
        boolean match = true;
        for (byte b : locationOnBus) {
          if (b != Byte.parseByte(address.get(i++))) {
            match = false;
            break;
          }
        }
        if (match)
          return new UsbRelayBoard(allDevices.get(physicalLocation));
      }
    }
    return null;
  }

  public UsbRelayBoard(UsbDevice device) {
    this.device = device;
  }

  public void on(int relayNumber) throws UsbException {
    logger.trace("Relay ON: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFF, (byte)relayNumber, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException e) {
      logger.catching(Level.ERROR, e);
      device.reset();
    }
  }

  public void off(int relayNumber) throws UsbException {
    logger.trace("Relay OFF: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFD, (byte)relayNumber, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException | IllegalStateException e) {
      logger.catching(Level.ERROR, e);
      device.reset();
    }
  }

  public UsbDevice getDevice() {
    return device;
  }

  public void setDevice(UsbDevice device) {
    this.device = device;
  }

  public int compareTo(UsbRelayBoard urb) {
    if (null == urb)
      return -1;

    return this.toString().compareTo(urb.toString());
  }

  public String toString() {
    return device.getPhysicalLocation().toString();
  }
}
