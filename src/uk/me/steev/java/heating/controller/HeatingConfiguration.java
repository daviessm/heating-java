package uk.me.steev.java.heating.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    } catch (JSONException jsone) {
      throw new HeatingException("Cannot get configuration", jsone);
    } catch (IOException ioe) {
      throw new HeatingException("Cannot get configuration", ioe);
    }
  }
  
  public String getSetting(String category, String setting) throws HeatingException {
    try {
      return CONFIGURATION.getJSONObject(category).getString(setting);
    } catch (JSONException jsone) {
      throw new HeatingException(category + "." + setting + " not found", jsone);
    }
  }

  public String[] getRelay(String name) throws HeatingException {
    try {
      JSONArray result = CONFIGURATION.getJSONObject("relays").getJSONArray(name);
      ArrayList<String> strings = new ArrayList<String>();
      for (int i = 0; i < result.length(); i++) {
        strings.add(String.valueOf(result.getInt(i)));
      }
      String[] array = new String[1];
      return strings.toArray(array);
    } catch (JSONException jsone) {
      throw new HeatingException("relays." + name + " not found", jsone);
    }
  }
}
