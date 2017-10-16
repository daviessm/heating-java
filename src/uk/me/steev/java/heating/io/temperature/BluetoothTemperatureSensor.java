package uk.me.steev.java.heating.io.temperature;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;

public class BluetoothTemperatureSensor {
  protected BluetoothDevice device;
  protected String name;
  protected List<BluetoothGattService> services;
  protected Map<String, BluetoothGattCharacteristic> characteristics;
  protected float currentTemperature;
  protected LocalDateTime tempLastUpdated;
  protected TemperatureUpdater temperatureUpdater;
  
  protected BluetoothTemperatureSensor(BluetoothDevice device) {
    this.temperatureUpdater = new TemperatureUpdater();

    if (null == device)
      return;
    
    this.device = device;
    this.name = this.device.getName();
    
    this.device.connect();
    this.services = this.device.getServices();

    this.characteristics = new HashMap<String, BluetoothGattCharacteristic>();
    for (BluetoothGattService service : this.services) {
      List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
      for (BluetoothGattCharacteristic characteristic : characteristics) {
        this.characteristics.put(characteristic.getUUID(), characteristic);
      }
    }
  }
  
  protected void disconnect() {
    device.disconnect();
  }
  
  protected void writeToUuid(String uuid, byte[] data) throws BluetoothException {
    if (this.characteristics.containsKey(uuid)) {
      this.characteristics.get(uuid).writeValue(data);
    } else {
      throw new BluetoothException("Unable to find characteristic " + uuid + " to write to");
    }
  }
  
  protected byte[] readFromUuid(String uuid) throws BluetoothException {
    if (this.characteristics.containsKey(uuid)) {
      return this.characteristics.get(uuid).readValue();
    }
    throw new BluetoothException("Unable to find characteristic " + uuid + " to read from");
  }
  
  private static BluetoothTemperatureSensor getSensorForDevice(BluetoothDevice device) {
    switch(device.getName()) {
    case "MetaWear":
      return new MetaWearSensor(device);
    case "SensorTag 2.0":
      return new SensorTagSensor(device);
    default:
      System.out.println("Unknown device " + device.getName());
      return null;
    }
  }
  
  public static Map<String,BluetoothTemperatureSensor> scanForSensors(Map<String,BluetoothTemperatureSensor> currentDevices) {
    Map<String,BluetoothTemperatureSensor> allDevices = new HashMap<>();

    /*BluetoothManager manager = BluetoothManager.getBluetoothManager();
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
          BluetoothTemperatureSensor sensor = getSensorForDevice(entry.getValue());
          if (null != sensor)
            allDevices.put(entry.getKey(), sensor);
        }
      }
    }
    return currentDevices;*/
    
    allDevices.put("dummy", new DummyTemperatureSensor(null));
    return allDevices;
  }
  
  public float getAmbientTemperature() throws BluetoothException {
    return 0f;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BluetoothDevice getDevice() {
    return device;
  }

  public void setDevice(BluetoothDevice device) {
    this.device = device;
  }

  public List<BluetoothGattService> getServices() {
    return services;
  }

  public void setServices(List<BluetoothGattService> services) {
    this.services = services;
  }

  public Map<String, BluetoothGattCharacteristic> getCharacteristics() {
    return characteristics;
  }

  public void setCharacteristics(Map<String, BluetoothGattCharacteristic> characteristics) {
    this.characteristics = characteristics;
  }

  public float getCurrentTemperature() {
    return currentTemperature;
  }

  public void setCurrentTemperature(float currentTemperature) {
    this.currentTemperature = currentTemperature;
  }

  public LocalDateTime getTempLastUpdated() {
    return tempLastUpdated;
  }

  public void setTempLastUpdated(LocalDateTime tempLastUpdated) {
    this.tempLastUpdated = tempLastUpdated;
  }

  public TemperatureUpdater getTemperatureUpdater() {
    return temperatureUpdater;
  }

  public void setTemperatureUpdater(TemperatureUpdater temperatureUpdater) {
    this.temperatureUpdater = temperatureUpdater;
  }

  public class TemperatureUpdater implements Runnable {
    public void run() {
      try {
        currentTemperature = getAmbientTemperature();
        tempLastUpdated = LocalDateTime.now();
      } catch (BluetoothException be) {
        //TODO
        be.printStackTrace();
      }
    }
  }
}
