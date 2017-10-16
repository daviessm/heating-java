package uk.me.steev.java.heating.io.temperature;

import tinyb.BluetoothDevice;

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
  
  public float getAmbientTemperature() throws BluetoothException {
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_ON_1);
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_ON_2);
    this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", TEMP_SENSOR_ON);
    
    for (int x = 0; x < 4; x++) {
      try {
        Thread.sleep(200);
        byte[] result = this.readFromUuid("326a9001-85cb-9195-d9dd-464cfbbae75a");
  
        int ambientTempRaw = (result[6] & 0xff) | (result[7] << 8);
        if (ambientTempRaw == 0)
          continue;
  
        float ambientTempCelsius = ambientTempRaw / 8f;
        
        this.writeToUuid("326a9001-85cb-9195-d9dd-464cfbbae75a", RED_LED_OFF);

        return ambientTempCelsius;
      } catch (InterruptedException ie) {
        throw new BluetoothException("Interrupted in Thread.sleep()", ie);
      }
    }
    throw new BluetoothException("Could not get temperature");
  }
}
