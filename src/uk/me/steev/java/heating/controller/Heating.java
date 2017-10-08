package uk.me.steev.java.heating.controller;

import java.util.Map;

import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class Heating {

  public static void main(String[] args) {
    Map<String,BluetoothTemperatureSensor> sensors = BluetoothTemperatureSensor.scanForSensors(null);
    
    for (BluetoothTemperatureSensor sensor : sensors.values()) {
      System.out.println(sensor.getName());
    }
  }
}
