package uk.me.steev.java.heating.io.temperature;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

public class MetaWearSensor extends BluetoothTemperatureSensor {
  private byte[] RED_LED_ON_1 = new byte[]{
      (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x02,
      (byte)0x1f, (byte)0x1f, (byte)0x00, (byte)0x00,
      (byte)0xd0, (byte)0x07, (byte)0x00, (byte)0x00,
      (byte)0xd0, (byte)0x07, (byte)0x00, (byte)0x00,
      (byte)0xff};
  private byte[] RED_LED_ON_2 = new byte[]{
      (byte)0x02, (byte)0x01, (byte)0x02};
  private byte[] TEMP_SENSOR_ON = new byte[]{(byte)0x04, (byte)0x81, (byte)0x01};
  private byte[] RED_LED_OFF = new byte[]{(byte)0x02, (byte)0x02, (byte)0x01};
  @SuppressWarnings("unused")
  private byte[] TEMP_SENSOR_OFF = new byte[]{}; //TODO figure this out
  
  public MetaWearSensor(BluetoothDevice device) {
    super(device);
  }
  
  @Override
  protected void updateTemperatureFromBluetooth() throws BluetoothException {
    logger.trace("RED_LED_ON_1 for " + this.toString());
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_ON_1);
    logger.trace("RED_LED_ON_2 for " + this.toString());
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_ON_2);
    logger.trace("TEMP_SENSOR_ON for " + this.toString());
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", TEMP_SENSOR_ON);
    
    for (int x = 0; x < 4; x++) {
      try {
        Thread.sleep(25);
        byte[] result = this.readFromUuid("326a9006-85cb-9195-d9dd-464cfbbae75a");
  
        int ambientTempRaw = (result[3] & 0xff) | (result[4] << 8);
        if (ambientTempRaw == 0)
          continue;
  
        float ambientTempCelsius = ambientTempRaw / 8f;
        
        logger.trace("RED_LED_OFF for " + this.toString() + " after " + x + " iterations");
        this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_OFF);

        this.currentTemperature = ambientTempCelsius;
        return;
      } catch (InterruptedException ie) {
        throw new BluetoothException("Interrupted in Thread.sleep()", ie);
      }
    }
    throw new BluetoothException("Could not get temperature");
  }
}
