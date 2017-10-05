package uk.me.steev.java.heating.controller;

import java.util.Map;

import uk.me.steev.java.heating.io.temperature.TemperatureSensor;

public class Heating {

  public static void main(String[] args) {
    Map<String,TemperatureSensor> sensors = TemperatureSensor.scanForSensors(null);
    
    for (TemperatureSensor sensor : sensors.values()) {
      System.out.println(sensor.getName());
    }
  }
}
