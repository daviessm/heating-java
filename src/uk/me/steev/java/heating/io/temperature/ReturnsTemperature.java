package uk.me.steev.java.heating.io.temperature;

public interface ReturnsTemperature {
  public float getAmbientTemperature() throws BluetoothException;
}
