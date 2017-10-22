package uk.me.steev.java.heating.io.temperature;

import tinyb.BluetoothDevice;

public class DummyTemperatureSensor extends BluetoothTemperatureSensor {
  protected DummyTemperatureSensor(BluetoothDevice device) throws BluetoothException {
    super(device);
  }

  protected void updateTemperatureFromBluetooth() throws BluetoothException {
    this.currentTemperature = 12f;
  }
}
