package uk.me.steev.java.heating.io.temperature;

public class BluetoothException extends Exception {
  private static final long serialVersionUID = -444197323556932711L;

  public BluetoothException(String msg) {
    super(msg);
  }
  
  public BluetoothException(String msg, Throwable thr) {
    super(msg, thr);
  }
}
