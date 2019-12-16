package uk.me.steev.java.heating.io.boiler.usb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

public class UsbUtils {
  private static Context CONTEXT = null;
  private static boolean INITIALISED = false;

  private UsbUtils() {
  }

  private static void init() throws UsbException {
    if (null == CONTEXT) {
      CONTEXT = new Context();
    }
    INITIALISED = (LibUsb.init(CONTEXT) >= 0);
    if (!INITIALISED)
      throw new UsbException("Unable to initialise libusb");

    DEVICE_LIST = new DeviceList();
    if (LibUsb.getDeviceList(CONTEXT, DEVICE_LIST) < 0)
       throw new UsbException("Unable to get device list");

    INITIALISED = true;
  }

  public static void reinitialiseDevices() throws UsbException {
    LibUsb.freeDeviceList(DEVICE_LIST, true);

    DEVICE_LIST = new DeviceList();
    if (LibUsb.getDeviceList(CONTEXT, DEVICE_LIST) < 0)
       throw new UsbException("Unable to get device list");
  }

  public static List<UsbDevice> findDevices(String idVendor, String idProduct) throws UsbException {
    if (!INITIALISED)
      init();

    List<UsbDevice> usbDevices = new ArrayList<UsbDevice>();
    DeviceList deviceList = new DeviceList();
    if (LibUsb.getDeviceList(CONTEXT, deviceList) < 0)
       throw new UsbException("Unable to get device list");
    for (Device d : deviceList) {
      DeviceDescriptor descriptor = new DeviceDescriptor();
      if (LibUsb.getDeviceDescriptor(d, descriptor) != 0)
        throw new UsbException("Unable to get device descriptor for device " + d.toString());

      if (descriptor.idVendor() == Short.parseShort(idVendor, 16) &&
          descriptor.idProduct() == Short.parseShort(idProduct, 16))
        usbDevices.add(new UsbDevice(d, descriptor));
    }
    LibUsb.freeDeviceList(deviceList, true);
    return usbDevices;
  }

  public static byte[] getDevicePhysicalLocation(UsbDevice device) throws UsbException {
    if (!INITIALISED)
      init();

    ByteBuffer buffer = ByteBuffer.allocateDirect(7);
    int numFilled = LibUsb.getPortNumbers(device.getDevice(), buffer);
    if (numFilled <= 0)
      throw new UsbException("Unable to get port numbers for device " + device.toString());

    byte[] ret = new byte[numFilled];
    buffer.get(ret);
    return ret;
  }
}
