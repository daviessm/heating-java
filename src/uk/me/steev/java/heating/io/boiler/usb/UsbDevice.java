package uk.me.steev.java.heating.io.boiler.usb;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

public class UsbDevice {
  static final Logger logger = LogManager.getLogger(UsbDevice.class.getName());
  protected Device device;
  protected DeviceHandle handle;
  protected UsbPhysicalLocation physicalLocation;
  boolean isOpen = false;
  
  public UsbDevice(Device device, DeviceDescriptor descriptor) throws UsbException {
    this.device = device;
    this.physicalLocation = new UsbPhysicalLocation(LibUsb.getBusNumber(device), UsbUtils.getDevicePhysicalLocation(this));
    this.handle = new DeviceHandle();

    int ret = 0;
    if ((ret = LibUsb.open(device, this.handle)) < 0)
      throw new UsbException("Unable to open device " + this.toString() + ": " + LibUsb.errorName(ret));

    if (LibUsb.kernelDriverActive(handle, 0) == 1)
      if ((ret = LibUsb.detachKernelDriver(handle, 0)) < 0)
        throw new UsbException("Unable to detach kernel driver for " + this.toString() + ": " + LibUsb.errorName(ret));

    if ((ret = LibUsb.claimInterface(this.handle, 0)) < 0)
      throw new UsbException("Unable to claim interface 0 on device " + this.toString() + ": " + LibUsb.errorName(ret));

    this.isOpen = true;
  }

  public void controlTransfer(byte[] data) throws UsbException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
    buffer.put(data);
    int ret = 0;
    try {
      ret = LibUsb.controlTransfer(this.handle, (byte)0x21, (byte)0x09, (short)768, (short)0, buffer, 1000);
    } catch (IllegalStateException e) {
      throw new UsbException("Exception sending control transfer message for device " + this.device, e);
    }
    if (ret < 0) {
      throw new UsbException("Unable to send control transfer message for device " + this.device + ": " + LibUsb.errorName(ret));
    }
  }

  public void reset() throws UsbException {
    int ret = 0;
    try {
      if ((ret = LibUsb.releaseInterface(this.handle, 0)) < 0)
        logger.warn("Unable to release interface 0 on device " + this.toString() + ": " + LibUsb.errorName(ret));
      LibUsb.close(this.handle);
    } catch (IllegalStateException e) {
      //This is not a fatal error, carry on closing the handle anyway
    }

    this.isOpen = false;

    this.handle = new DeviceHandle();
    if ((ret = LibUsb.open(device, this.handle)) < 0)
      throw new UsbException("Unable to open device " + this.toString() + ": " + LibUsb.errorName(ret));

    if (LibUsb.kernelDriverActive(handle, 0) == 1)
      if ((ret = LibUsb.detachKernelDriver(handle, 0)) < 0)
        throw new UsbException("Unable to detach kernel driver for " + this.toString() + ": " + LibUsb.errorName(ret));

    if ((ret = LibUsb.claimInterface(this.handle, 0)) < 0)
      throw new UsbException("Unable to claim interface 0 on device " + this.toString() + ": " + LibUsb.errorName(ret));

    this.isOpen = true;
  }

  public Device getDevice() {
    return device;
  }

  public void setDevice(Device device) {
    this.device = device;
  }

  public UsbPhysicalLocation getPhysicalLocation() {
    return physicalLocation;
  }

  public void setPhysicalLocation(UsbPhysicalLocation physicalLocation) {
    this.physicalLocation = physicalLocation;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("UsbDevice [device=").append(device)
        .append(", physicalLocation=").append(physicalLocation).append("]");
    return builder.toString();
  }
}
