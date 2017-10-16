package uk.me.steev.java.heating.io.temperature;

import tinyb.BluetoothDevice;

public class SensorTagSensor extends BluetoothTemperatureSensor {

  public SensorTagSensor(BluetoothDevice device) {
    super(device);
  }
  
  public float getAmbientTemperature() throws BluetoothException {
    //Red LED on
    this.writeToUuid("f000aa65-0451-4000-b000-000000000000", new byte[]{(byte)0x01});
    this.writeToUuid("f000aa66-0451-4000-b000-000000000000", new byte[]{(byte)0x01});
    
    //Temperature sensor on
    this.writeToUuid("f000aa02-0451-4000-b000-000000000000", new byte[]{(byte)0x01});
    
    for (int x = 0; x < 4; x++) {
      try {
        Thread.sleep(200);
        byte[] result = this.readFromUuid("f000aa01-0451-4000-b000-000000000000");
  
        int objectTempRaw = (result[0] & 0xff) | (result[1] << 8);
        int ambientTempRaw = (result[2] & 0xff) | (result[3] << 8);
        if (ambientTempRaw == 0)
          continue;
  
        @SuppressWarnings("unused")
        float objectTempCelsius = objectTempRaw / 128f;
        float ambientTempCelsius = ambientTempRaw / 128f;
        
        //Red LED off
        this.writeToUuid("f000aa65-0451-4000-b000-000000000000", new byte[]{(byte)0x00});
        this.writeToUuid("f000aa66-0451-4000-b000-000000000000", new byte[]{(byte)0x00});
        
        //Temperature sensor off
        this.writeToUuid("f000aa02-0451-4000-b000-000000000000", new byte[]{(byte)0x00});

        return ambientTempCelsius;
      } catch (InterruptedException ie) {
        throw new BluetoothException("Interrupted in Thread.sleep()", ie);
      }
    }
    throw new BluetoothException("Could not get temperature");
  }

}
