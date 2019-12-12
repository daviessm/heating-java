package uk.me.steev.java.heating.io.boiler.usb;

import java.util.Arrays;

public class UsbPhysicalLocation implements Comparable<UsbPhysicalLocation> {
  protected int busNumber;
  protected byte[] locationOnBus;

  public UsbPhysicalLocation(int busNumber, byte[] physicalLocation) {
    this.busNumber = busNumber;
    this.locationOnBus = physicalLocation;
  }

  public int getBusNumber() {
    return busNumber;
  }

  public void setBusNumber(int busNumber) {
    this.busNumber = busNumber;
  }

  public byte[] getLocationOnBus() {
    return locationOnBus;
  }

  public void setLocationOnBus(byte[] physicalLocation) {
    this.locationOnBus = physicalLocation;
  }

  public int compareTo(UsbPhysicalLocation upl) {
    if (null == upl)
      return -1;

    return this.toString().compareTo(upl.toString());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("UsbPhysicalLocation [busNumber=").append(busNumber).append(", physicalLocation=")
        .append(Arrays.toString(locationOnBus)).append("]");
    return builder.toString();
  }
}
