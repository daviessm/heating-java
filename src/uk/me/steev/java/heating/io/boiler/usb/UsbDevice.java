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
  protected DeviceDescriptor descriptor;
  protected DeviceHandle handle;
  protected UsbPhysicalLocation physicalLocation;
  boolean isOpen = false;
  
  public UsbDevice(Device device, DeviceDescriptor descriptor) throws UsbException {
    this.device = device;
    this.descriptor = descriptor;
    this.physicalLocation = new UsbPhysicalLocation(LibUsb.getBusNumber(device), UsbUtils.getDevicePhysicalLocation(this));
    this.handle = new DeviceHandle();

    if (LibUsb.open(device, this.handle) < 0)
      throw new UsbException("Unable to open device " + this.toString());

    if (LibUsb.claimInterface(this.handle, 0) < 0)
      throw new UsbException("Unable to claim interface 0 on device " + this.toString());

    if (LibUsb.kernelDriverActive(handle, 0) == 1)
      if (LibUsb.detachKernelDriver(handle, 0) < 0)
        throw new UsbException("Unable to detach kernel driver for " + this.toString());
    this.isOpen = true;
  }

  public void controlTransfer(byte[] data) throws UsbException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
    buffer.put(data);
    int ret = LibUsb.controlTransfer(this.handle, (byte)0x21, (byte)0x09, (short)768, (short)0, buffer, 1000);
    if (ret < 0) {
      logger.error(LibUsb.errorName(ret));
      throw new UsbException("Unable to send control transfer message for device " + this.device);
    }
  }

  public void reset() throws UsbException {
    LibUsb.close(this.handle);
    this.isOpen = false;

    this.handle = new DeviceHandle();
    if (LibUsb.open(device, this.handle) < 0)
      throw new UsbException("Unable to open device " + this.toString());
    
    if (LibUsb.claimInterface(this.handle, 0) < 0)
      throw new UsbException("Unable to claim interface 0 on device " + this.toString());

    if (LibUsb.kernelDriverActive(handle, 0) == 1)
      if (LibUsb.detachKernelDriver(handle, 0) < 0)
        throw new UsbException("Unable to detach kernel driver for " + this.toString());
    this.isOpen = true;

  }

  public Device getDevice() {
    return device;
  }

  public void setDevice(Device device) {
    this.device = device;
  }

  public DeviceDescriptor getDescriptor() {
    return descriptor;
  }

  public void setDescriptor(DeviceDescriptor descriptor) {
    this.descriptor = descriptor;
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
    builder.append("UsbDevice [device=").append(device).append(", descriptor=").append(descriptor)
        .append(", physicalLocation=").append(physicalLocation).append("]");
    return builder.toString();
  }
}
