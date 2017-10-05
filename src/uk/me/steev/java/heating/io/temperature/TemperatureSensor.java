package uk.me.steev.java.heating.io.temperature;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tinyb.BluetoothDevice;
import tinyb.BluetoothManager;

public class TemperatureSensor {
  protected BluetoothDevice device;
  protected String name;
  
  protected TemperatureSensor(BluetoothDevice device) {
    this.device = device;
    this.name = device.getName();
  }
  
  private static TemperatureSensor getSensorForDevice(BluetoothDevice device) {
    switch(device.getName()) {
    case "MetaWear":
      return new MetaWearSensor(device);
    default:
      System.out.println("Unknown device " + device.getName());
      return null;
    }
  }
  
  public static Map<String,TemperatureSensor> scanForSensors(Map<String,TemperatureSensor> currentDevices) {
    if (null == currentDevices)
      currentDevices = new HashMap<>();

    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    LocalDateTime startedAt = LocalDateTime.now();

    if (manager.startDiscovery()) {   
      Map<String,BluetoothDevice> newDevices = new HashMap<>();
      System.out.println("Started discovery");
      while(startedAt.plus(10, ChronoUnit.SECONDS).isAfter(LocalDateTime.now())){
        for(BluetoothDevice device : manager.getDevices()){
          newDevices.put(device.getAddress(), device);
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      for (Entry<String, BluetoothDevice> entry : newDevices.entrySet()) {
        if (!currentDevices.containsKey(entry.getKey())) {
          TemperatureSensor sensor = getSensorForDevice(entry.getValue());
          if (null != sensor)
            currentDevices.put(entry.getKey(), sensor);
        }
      }
    }
    return currentDevices;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
