package uk.me.steev.java.heating.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.me.steev.java.heating.io.boiler.RelayType;
import uk.me.steev.java.heating.utils.JSONUtils;

public class HeatingConfiguration {
  private static HeatingConfiguration SINGLETON = null;
  private static JSONObject CONFIGURATION = null;

  public static HeatingConfiguration getConfiguration(File configurationLocation) throws HeatingException {
    if (null == HeatingConfiguration.SINGLETON) {
      synchronized (HeatingConfiguration.class) {
        HeatingConfiguration.SINGLETON = new HeatingConfiguration(configurationLocation);
      }
    }
    return HeatingConfiguration.SINGLETON;
  }

  private HeatingConfiguration(File configurationLocation) throws HeatingException {
    try {
      HeatingConfiguration.CONFIGURATION = JSONUtils.readJsonFromFile(configurationLocation);
    } catch (JSONException | IOException e) {
      throw new HeatingException("Cannot get configuration", e);
    }
  }

  public static String getStringSetting(String category, String setting) throws HeatingException {
    try {
      return CONFIGURATION.getJSONObject(category).getString(setting);
    } catch (JSONException jsone) {
      throw new HeatingException(category + "." + setting + " not found", jsone);
    }
  }

  public static int getIntegerSetting(String category, String setting) throws HeatingException {
    try {
      return CONFIGURATION.getJSONObject(category).getInt(setting);
    } catch (JSONException jsone) {
      throw new HeatingException(category + "." + setting + " not found", jsone);
    }
  }

  public static double getDoubleSetting(String category, String setting) throws HeatingException {
    try {
      return CONFIGURATION.getJSONObject(category).getDouble(setting);
    } catch (JSONException jsone) {
      throw new HeatingException(category + "." + setting + " not found", jsone);
    }
  }

  public static String[] getArray(String category, String setting) throws HeatingException {
    try {
      JSONArray result = CONFIGURATION.getJSONObject(category).getJSONArray(setting);
      ArrayList<String> strings = new ArrayList<String>();
      for (Object o : result) {
        strings.add(o.toString());
      }
      String[] array = new String[1];
      return strings.toArray(array);
    } catch (JSONException jsone) {
      throw new HeatingException(category + "." + setting + " not found", jsone);
    }
  }

  public static Map<String, RelayConfiguration> getRelayConfiguration() throws HeatingException {
    Map<String, RelayConfiguration> relays = new TreeMap<String, RelayConfiguration>();
    JSONObject relaysJSON = CONFIGURATION.getJSONObject("relays");
    for (String name : relaysJSON.keySet()) {
      JSONArray addressJSON = relaysJSON.getJSONObject(name).getJSONArray("bus_location");
      ArrayList<String> address = new ArrayList<String>();
      for (Object o : addressJSON) {
        address.add(o.toString());
      }
      int relayNumber = relaysJSON.getJSONObject(name).getInt("relay_number");

      RelayConfiguration relayConfiguration = new RelayConfiguration(RelayType.USB, address, relayNumber);
      relays.put(name, relayConfiguration);
    }
    return relays;
  }

  public static class RelayConfiguration {
    private RelayType relayType;
    private ArrayList<String> address;
    private int relayNumber;

    public RelayConfiguration(RelayType relayType, ArrayList<String> address, int relayNumber) {
      this.relayType = relayType;
      this.address = address;
      this.relayNumber = relayNumber;
    }

    public RelayType getRelayType() {
      return relayType;
    }

    public void setRelayType(RelayType relayType) {
      this.relayType = relayType;
    }

    public ArrayList<String> getAddress() {
      return address;
    }

    public void setAddress(ArrayList<String> address) {
      this.address = address;
    }

    public int getRelayNumber() {
      return relayNumber;
    }

    public void setRelayNumber(int relayNumber) {
      this.relayNumber = relayNumber;
    }
  }
}
