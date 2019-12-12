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
}
