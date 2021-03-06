package uk.me.steev.java.heating.io.api;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.utils.JSONUtils;

public class WeatherAdapter {
  static final Logger logger = LogManager.getLogger(WeatherAdapter.class.getName());
  protected JSONObject latestReading;
  protected HeatingConfiguration config;
  protected TemperatureUpdater updater;

  public WeatherAdapter() throws HeatingException {
    this.updater = new TemperatureUpdater();
  }

  protected void update() throws HeatingException {
    try {
      this.latestReading = JSONUtils.readJsonFromUrl("https://api.openweathermap.org/data/2.5/weather?appid=" +
                                                     HeatingConfiguration.getStringSetting("openweathermap", "api_key") +
                                                     "&lat=" +
                                                     HeatingConfiguration.getStringSetting("openweathermap", "lat") +
                                                     "&lon=" +
                                                     HeatingConfiguration.getStringSetting("openweathermap", "long"));
      logger.trace("Got response from OpenWeatherMap API: " + this.latestReading.toString());
    } catch (IOException ioe) {
      logger.catching(Level.WARN, ioe);
    }
  }

  public double getLatestTemperature() throws CallFailedException {
    if (null != this.latestReading) {
      try {
        return this.latestReading.getJSONObject("main").getDouble("temp") - 273.15;
      } catch (JSONException jsone) {
        throw new CallFailedException("Unable to find temperature", jsone);
      }
    }
    throw new CallFailedException("Stored JSON is null");
  }

  public double getApparentTemperature() throws CallFailedException {
    if (null != this.latestReading) {
      try {
        return this.latestReading.getJSONObject("main").getDouble("feels_like") - 273.15;
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
        logger.catching(Level.ERROR, he);
      }
    }
  }
}
