package uk.me.steev.java.heating.io.boiler.usb;

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
  
  public static Relay getRelay(String[] address) {
    if (address.length < 2)
      return null;

    Map<UsbPhysicalLocation, UsbRelay> relays = findRelays();
    for (UsbPhysicalLocation physicalLocation : relays.keySet()) {
      if (physicalLocation.getBusNumber() == Integer.parseInt(address[0])) {
        byte[] locationOnBus = physicalLocation.getLocationOnBus();
        if (locationOnBus.length != address.length - 1)
          continue;
        
        int i = 0;
        boolean match = true;
        for (byte b : locationOnBus) {
          if (b != Byte.parseByte(address[i++]))
            break;
        }
        if (match)
          return relays.get(physicalLocation);
      }
    }
    return null;
  }
  
  public static Map<UsbPhysicalLocation, UsbRelay> findRelays() {
    Map<UsbPhysicalLocation, UsbRelay> relays = new HashMap<UsbPhysicalLocation, UsbRelay>();
    List<UsbDevice> devices = null;
    
    try {
      devices = UsbUtils.findDevices("16C0", "05DF");
      for (UsbDevice device : devices) {
        relays.put(device.getPhysicalLocation(), new UsbRelay(device));
      }
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }
    return relays;
  }

  protected UsbRelay(UsbDevice device) {
    this.device = device;
    this.off();
  }
  
  public void on() {
    logger.trace("Relay ON: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFE, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException ue) {
      logger.catching(Level.ERROR, ue);
    }
    this.on = true;
  }
  
  public void off() {
    logger.trace("Relay OFF: " + this.toString());
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFC, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
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
