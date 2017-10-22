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

public abstract class BluetoothTemperatureSensor {
  static final Logger logger = LogManager.getLogger(BluetoothTemperatureSensor.class.getName());
  protected BluetoothDevice device = null;
  protected String name = null;
  protected List<BluetoothGattService> services = null;
  protected Map<String, BluetoothGattCharacteristic> characteristics = null;
  protected Float currentTemperature = null;
  protected LocalDateTime tempLastUpdated = null;
  protected LocalDateTime tempLastFailedUpdate = null;
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
      logger.trace("Connecting to " + this.toString());
      try {
        if (!this.device.connect())
          throw new BluetoothException("Unable to connect to " + this.toString());
      } catch (tinyb.BluetoothException bte1) {
        logger.warn("Unable to connect to " + this.toString());
        try {
          this.device.disconnect();
        } catch (tinyb.BluetoothException bte2) {
          throw new BluetoothException("Unable to disconnect from " + this.toString() + " either", bte2);
        }
      }

      LocalDateTime startTime = LocalDateTime.now();
      while (LocalDateTime.now().isBefore(startTime.plusSeconds(30))) {
        if (this.device.getServicesResolved()) {
          logger.debug("Got characteristics after " + ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()) + "ms");
          break;
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException ie) {
          throw new BluetoothException("Interrupted in Thread.sleep()", ie);
        }
      }
      if (!this.device.getServicesResolved()) {
        try {
          this.device.disconnect();
        } catch (tinyb.BluetoothException bte) {
          throw new BluetoothException("Unable to disconnect from " + this.toString(), bte);
        }
        throw new BluetoothException("Unable to get services for " + this.toString());
      }

      logger.trace("Getting services for " + this.toString());
      this.services = this.device.getServices();
      if (null == this.services || this.services.size() == 0) {
        this.services = null;
        throw new BluetoothException("Found no services for " + this.toString());
      }

      logger.trace("Getting characteristics for " + this.toString());
      this.characteristics = new HashMap<String, BluetoothGattCharacteristic>();
      for (BluetoothGattService service : this.services) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
          this.characteristics.put(characteristic.getUUID(), characteristic);
        }
      }
      if (null == this.characteristics || this.characteristics.size() == 0) {
        this.characteristics = null;
        throw new BluetoothException("Found no characteristics for " + this.toString());
      }

      logger.trace("Got characteristics for " + this.toString());
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
      throw new BluetoothException("Unable to find characteristic " + uuid + " to write to; known characteristics: " + this.characteristics);
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
    throw new BluetoothException("Unable to find characteristic " + uuid + " to read from; known characteristics: " + this.characteristics);
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

  protected void updateTemperature() throws BluetoothException {
    this.tempLastFailedUpdate = LocalDateTime.now();
    updateTemperatureFromBluetooth();
    this.tempLastUpdated = LocalDateTime.now();
    this.tempLastFailedUpdate = null;
  }

  protected abstract void updateTemperatureFromBluetooth() throws BluetoothException;

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

  public LocalDateTime getTempLastFailedUpdate() {
    return tempLastFailedUpdate;
  }

  public void setTempLastFailedUpdate(LocalDateTime tempLastFailedUpdate) {
    this.tempLastFailedUpdate = tempLastFailedUpdate;
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("BluetoothTemperatureSensor ").append(name).append(" at ").append(this.device.getAddress())
        .append(" [currentTemperature=").append(currentTemperature).append(", tempLastUpdated=").append(tempLastUpdated)
        .append(", tempLastFailedUpdate=").append(tempLastFailedUpdate).append("]");
    return builder.toString();
  }

  public class TemperatureUpdater implements Runnable {
    public void run() {
      try {
        try {
          updateTemperature();
          logger.info("Got temperature " + currentTemperature + " for device " + device.getAddress());
        } catch (BluetoothException be) {
          logger.catching(Level.WARN, be);
        }
      } catch (Throwable t) {
        logger.catching(Level.ERROR, t);
      }
    }
  }
}
