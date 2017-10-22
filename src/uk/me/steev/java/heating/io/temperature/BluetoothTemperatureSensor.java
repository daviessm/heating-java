package uk.me.steev.java.heating.io.temperature;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;

public class BluetoothTemperatureSensor {
  static final Logger logger = LogManager.getLogger(BluetoothTemperatureSensor.class.getName());
  protected BluetoothDevice device = null;
  protected String name = null;
  protected List<BluetoothGattService> services = null;
  protected Map<String, BluetoothGattCharacteristic> characteristics = null;
  protected Float currentTemperature = null;
  protected LocalDateTime tempLastUpdated = null;
  protected TemperatureUpdater temperatureUpdater = null;
  protected ScheduledFuture<?> temperatureUpdatdaterFuture = null;
  
  protected BluetoothTemperatureSensor(BluetoothDevice device){
    this.temperatureUpdater = new TemperatureUpdater();

    if (null == device)
      return;
    
    this.device = device;
    this.name = this.device.getName();
  }
  
  private void populateServicesAndCharacteristics() throws BluetoothException {
    try {
      this.device.connect();
      this.services = this.device.getServices();
  
      this.characteristics = new HashMap<String, BluetoothGattCharacteristic>();
      for (BluetoothGattService service : this.services) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
          this.characteristics.put(characteristic.getUUID(), characteristic);
        }
      }
    } catch (tinyb.BluetoothException bte) {
      throw new BluetoothException("Unable to get Bluetooth details for " + this.toString(), bte);
    }
  }
  
  public void disconnect() {
    logger.warn("Disconnecting from " + this.toString());
    device.disconnect();
  }
  
  protected void writeToUuid(String uuid, byte[] data) throws BluetoothException {
    if (null == this.characteristics)
      populateServicesAndCharacteristics();
    if (this.characteristics.containsKey(uuid)) {
      try {
        this.characteristics.get(uuid).writeValue(data);
      } catch (tinyb.BluetoothException bte) {
        throw new BluetoothException("Unable to write to UUID", bte);
      }
    } else {
      throw new BluetoothException("Unable to find characteristic " + uuid + " to write to");
    }
  }
  
  protected byte[] readFromUuid(String uuid) throws BluetoothException {
    if (null == this.characteristics)
      populateServicesAndCharacteristics();
    if (this.characteristics.containsKey(uuid)) {
      try {
        return this.characteristics.get(uuid).readValue();
      } catch (tinyb.BluetoothException bte) {
        throw new BluetoothException("Unable to write to UUID", bte);
      }
    }
    throw new BluetoothException("Unable to find characteristic " + uuid + " to read from");
  }
  
  private static BluetoothTemperatureSensor getSensorForDevice(BluetoothDevice device) {
    BluetoothTemperatureSensor sensor = null;
    switch(device.getName()) {
    case "MetaWear":
      sensor = new MetaWearSensor(device);
      break;
    case "CC2650 SensorTag":
    case "SensorTag 2.0":
      sensor = new SensorTagSensor(device);
      break;
    default:
      logger.info("Unknown device " + device.getName());
    }
    return sensor;
  }
  
  public static Map<String,BluetoothTemperatureSensor> scanForSensors() {
    Map<String,BluetoothTemperatureSensor> allSensors = new HashMap<>();

    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    LocalDateTime startedAt = LocalDateTime.now();

    logger.debug("Start scanning for devices");
    if (manager.startDiscovery()) {   
      Map<String,BluetoothDevice> newDevices = new HashMap<>();
      while (startedAt.plus(10, ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
        for (BluetoothDevice device : manager.getDevices()) {
          newDevices.put(device.getAddress(), device);
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.catching(Level.WARN, e);
        }
      }
      logger.debug("Stop scanning for devices");
      manager.stopDiscovery();

      for (Entry<String, BluetoothDevice> entry : newDevices.entrySet()) {
        BluetoothTemperatureSensor sensor = getSensorForDevice(entry.getValue());
        if (null != sensor) {
          logger.trace("Found device " + sensor.getName() + " at " + entry.getKey());
          allSensors.put(entry.getKey(), sensor);
        }
      }
    }
    
    //allDevices.put("dummy", new DummyTemperatureSensor(null));
    return allSensors;
  }
  
  protected float getAmbientTemperature() throws BluetoothException {
    logger.warn("Getting temperature for superclass!");
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

  public Float getCurrentTemperature() {
    return currentTemperature;
  }

  public void setCurrentTemperature(Float currentTemperature) {
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
  
  public ScheduledFuture<?> getTemperatureUpdatdaterFuture() {
    return temperatureUpdatdaterFuture;
  }

  public void setTemperatureUpdatdaterFuture(ScheduledFuture<?> temperatureUpdatdaterFuture) {
    this.temperatureUpdatdaterFuture = temperatureUpdatdaterFuture;
  }

  public String toString() {
    return this.device.getName() + " at " + this.device.getAddress();
  }

  public class TemperatureUpdater implements Runnable {
    public void run() {
      try {
        try {
          currentTemperature = getAmbientTemperature();
          tempLastUpdated = LocalDateTime.now();
          logger.info("Got temperature " + currentTemperature + " for device " + device.getAddress());
        } catch (BluetoothException be) {
          logger.catching(Level.WARN, be);
          disconnect();
          getTemperatureUpdatdaterFuture().cancel(false);
        }
      } catch (Throwable t) {
        logger.catching(Level.ERROR, t);
      }
    }
  }
}
