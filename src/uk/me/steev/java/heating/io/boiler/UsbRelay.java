package uk.me.steev.java.heating.io.boiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.me.steev.java.heating.io.boiler.usb.UsbDevice;
import uk.me.steev.java.heating.io.boiler.usb.UsbException;
import uk.me.steev.java.heating.io.boiler.usb.UsbPhysicalLocation;
import uk.me.steev.java.heating.io.boiler.usb.UsbUtils;

public class UsbRelay extends Relay {
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
      ue.printStackTrace();
    }
    return relays;
  }

  public UsbRelay(UsbDevice device) {
    this.device = device;
  }
  
  public void on() {
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFE, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException ue) {
      //TODO handle this
      ue.printStackTrace();
    }
    this.on = true;
  }
  
  public void off() {
    try {
      this.device.controlTransfer(new byte[]{(byte)0xFC, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
    } catch (UsbException ue) {
      //TODO handle this
      ue.printStackTrace();
    }
    this.on = false;
  }
  
  public boolean isOn() {
    return on;
  }
}
