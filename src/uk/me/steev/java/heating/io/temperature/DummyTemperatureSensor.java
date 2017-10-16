package uk.me.steev.java.heating.io.temperature;

import tinyb.BluetoothDevice;

public class DummyTemperatureSensor extends BluetoothTemperatureSensor {
  protected DummyTemperatureSensor(BluetoothDevice device) {
    super(device);
  }

  public float getAmbientTemperature() throws BluetoothException {
    return 5f;
  }
}
