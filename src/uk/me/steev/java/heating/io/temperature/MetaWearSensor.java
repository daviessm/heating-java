package uk.me.steev.java.heating.io.temperature;

import tinyb.BluetoothDevice;

public class MetaWearSensor extends TemperatureSensor {
  public MetaWearSensor(BluetoothDevice device) {
    super(device);
  }
}
