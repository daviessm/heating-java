package uk.me.steev.java.heating.io.api;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.utils.JSONUtils;

public class WeatherAdapter {
  protected JSONObject latestReading;
  protected HeatingConfiguration config;
  protected TemperatureUpdater updater;
  
  public WeatherAdapter(HeatingConfiguration config) throws HeatingException {
    this.config = config;
    this.updater = new TemperatureUpdater();
  }
  
  public void update() throws HeatingException {
    try {
      this.latestReading = JSONUtils.readJsonFromUrl("https://api.darksky.net/forecast/" +
                                                     this.config.getSetting("darksky", "api_key") + 
                                                     "/" + 
                                                     this.config.getSetting("darksky", "latlong") +
                                                     "?exclude=[minutely,hourly,daily]&units=si");
    } catch (IOException ioe) {
      //TODO log
      ioe.printStackTrace();
    } catch (HeatingException he) {
      throw he;
    }
  }
  
  public double getLatestTemperature() throws CallFailedException {
    if (null != this.latestReading) {
      try {
        return this.latestReading.getJSONObject("currently").getDouble("temperature");
      } catch (JSONException jsone) {
        throw new CallFailedException("Unable to find temperature", jsone);
      }
    }
    throw new CallFailedException("Stored JSON is null");
  }

  public double getApparentTemperature() throws CallFailedException {
    if (null != this.latestReading) {
      try {
        return this.latestReading.getJSONObject("currently").getDouble("apparentTemperature");
      } catch (JSONException jsone) {
        throw new CallFailedException("Unable to find apparentTemperature", jsone);
      }
    }
    throw new CallFailedException("Stored JSON is null");
  }
  
  public JSONObject getLatestReading() {
    return latestReading;
  }

  public void setLatestReading(JSONObject latestReading) {
    this.latestReading = latestReading;
  }

  public HeatingConfiguration getConfig() {
    return config;
  }

  public void setConfig(HeatingConfiguration config) {
    this.config = config;
  }

  public TemperatureUpdater getUpdater() {
    return updater;
  }

  public void setUpdater(TemperatureUpdater updater) {
    this.updater = updater;
  }

  public class TemperatureUpdater implements Runnable {
    public void run() {
      try {
        update();
      } catch (HeatingException he) {
        //TODO
        he.printStackTrace();
      }
    }
  }
}
