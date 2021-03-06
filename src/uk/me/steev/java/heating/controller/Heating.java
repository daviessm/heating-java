package uk.me.steev.java.heating.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import info.faljse.SDNotify.SDNotify;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import uk.me.steev.java.heating.io.api.CalendarAdapter;
import uk.me.steev.java.heating.io.api.WeatherAdapter;
import uk.me.steev.java.heating.io.boiler.Boiler;
import uk.me.steev.java.heating.io.boiler.RelayException;
import uk.me.steev.java.heating.io.http.HttpAdapter;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor.TemperatureUpdater;
import uk.me.steev.java.heating.utils.Processable;
import uk.me.steev.java.heating.utils.ResubmittingScheduledExecutor;

public class Heating {
  private static final Logger logger = LogManager.getLogger(Heating.class.getName());
  protected Boiler boiler;
  protected WeatherAdapter weather;
  protected CalendarAdapter calendar;
  protected Map<String,BluetoothTemperatureSensor> sensors;
  protected ScheduledThreadPoolExecutor scheduledExecutor;
  protected SensorScanner scanner;
  protected HeatingProcessor processor;
  protected HttpAdapter httpAdapter;
  protected Float desiredTemperature;
  protected Float proportion;
  protected Float overrideDegrees = 0f;
  protected LocalDateTime overrideEnd = LocalDateTime.now();
  protected LocalDateTime goneOutUntilTime = null;

  public Heating(File configFile) throws HeatingException {
    try {
      logger.info("Starting up");

      //Set up configuration
      HeatingConfiguration.getConfiguration(configFile);
      logger.info("Got configuration");

      //Set up boiler (which sets up the relays)
      this.boiler = new Boiler();
      logger.info("Set up boiler");

      //Set up event processor
      this.processor = new HeatingProcessor(this);
      logger.info("Set up heating processor");

      //Set up weather API
      this.weather = new WeatherAdapter();
      logger.info("Set up weather adapter");

      //Set up events API
      this.calendar = new CalendarAdapter(this.processor);
      logger.info("Set up calendar");

      //Set up an empty set of temperature sensors
      this.sensors = new ConcurrentHashMap<>();

      //Set up a thing to run other things periodically
      this.scheduledExecutor = new ResubmittingScheduledExecutor(10);

      this.scanner = new SensorScanner(this.processor);

      this.httpAdapter = HttpAdapter.getHttpAdapter(this);
      logger.info("Set up HTTP adapter");

      //Notify systemd (if running) that startup has finished
      SDNotify.sendNotify();
      logger.info("Notified SystemD that we're running");
    } catch (RelayException e) {
      throw new HeatingException("Error creating Heating object", e);
    }
  }

  public Heating() throws HeatingException {
    this(new File("config.json"));
  }

  public void start() throws IOException, HeatingException {
    logger.info("Starting");
    //Get initial calendar information
    this.calendar.init();
    logger.info("Initialised calendar");

    //Look for new sensors every minute
    this.scheduledExecutor.scheduleAtFixedRate(this.scanner, 0, 1, TimeUnit.MINUTES);
    logger.info("Scheduled sensor scanner");

    //Get new events every six hours
    this.scheduledExecutor.scheduleAtFixedRate(calendar.getEventsUpdater(), 0, 6, TimeUnit.HOURS);
    logger.info("Scheduled calendar updates");

    //Get new outside temperature every fifteen minutes
    this.scheduledExecutor.scheduleAtFixedRate(weather.getUpdater(), 0, 15, TimeUnit.MINUTES);
    logger.info("Scheduled weather updates");

    //Process the whole lot every minute
    this.scheduledExecutor.scheduleWithFixedDelay(this.processor, 0, 1, TimeUnit.MINUTES);
    logger.info("Scheduled processor...startup complete!");
  }

  public Boiler getBoiler() {
    return boiler;
  }

  public void setBoiler(Boiler boiler) {
    this.boiler = boiler;
  }

  public WeatherAdapter getWeather() {
    return weather;
  }

  public void setWeather(WeatherAdapter weather) {
    this.weather = weather;
  }

  public CalendarAdapter getCalendar() {
    return calendar;
  }

  public void setCalendar(CalendarAdapter calendar) {
    this.calendar = calendar;
  }

  public Map<String, BluetoothTemperatureSensor> getSensors() {
    return sensors;
  }

  public void setSensors(Map<String, BluetoothTemperatureSensor> sensors) {
    this.sensors = sensors;
  }

  public ScheduledThreadPoolExecutor getScheduledExecutor() {
    return scheduledExecutor;
  }

  public void setScheduledExecutor(ScheduledThreadPoolExecutor scheduledExecutor) {
    this.scheduledExecutor = scheduledExecutor;
  }

  public SensorScanner getScanner() {
    return scanner;
  }

  public void setScanner(SensorScanner scanner) {
    this.scanner = scanner;
  }

  public HeatingProcessor getProcessor() {
    return processor;
  }

  public void setProcessor(HeatingProcessor processor) {
    this.processor = processor;
  }

  public HttpAdapter getHttpAdapter() {
    return httpAdapter;
  }

  public void setHttpAdapter(HttpAdapter httpAdapter) {
    this.httpAdapter = httpAdapter;
  }

  public Float getDesiredTemperature() {
    return desiredTemperature;
  }

  public void setDesiredTemperature(Float desiredTemperature) {
    this.desiredTemperature = desiredTemperature;
  }

  public Float getProportion() {
    return proportion;
  }

  public void setProportion(Float proportion) {
    this.proportion = proportion;
  }

  public Float getOverrideDegrees() {
    return overrideDegrees;
  }

  public void setOverrideDegrees(Float overrideDegrees) {
    this.overrideDegrees = overrideDegrees;
  }

  public LocalDateTime getOverrideEnd() {
    return overrideEnd;
  }

  public void setOverrideEnd(LocalDateTime overrideEnd) {
    this.overrideEnd = overrideEnd;
  }

  public LocalDateTime getGoneOutUntilTime() {
    return goneOutUntilTime;
  }

  public void setGoneOutUntilTime(LocalDateTime goneOutUntilTime) {
    this.goneOutUntilTime = goneOutUntilTime;
  }

  public class SensorScanner implements Runnable {
    private Processable temperatureUpdatedCallback;

    public SensorScanner(Processable temperatureUpdatedCallback) {
      this.temperatureUpdatedCallback = temperatureUpdatedCallback;
    }

    public void run() {
      synchronized(boiler) {
        try {
          Map<String,BluetoothTemperatureSensor> newSensors = BluetoothTemperatureSensor.scanForSensors();
          synchronized (sensors) {
            for (Entry<String, BluetoothTemperatureSensor> entry : newSensors.entrySet()) {
              if (!sensors.containsKey(entry.getKey())) {
                BluetoothTemperatureSensor sensor = newSensors.get(entry.getKey());
                TemperatureUpdater task = sensor.getTemperatureUpdater();
                task.setCallback(temperatureUpdatedCallback);
                sensor.setTemperatureUpdatdaterFuture(scheduledExecutor.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES));
                logger.info("Adding " + sensor + " to sensors list");
                sensors.put(entry.getKey(), entry.getValue());
              }
            }
          }
        } catch (Throwable t) {
          logger.catching(Level.ERROR, t);
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Heating [config=").append(", boiler=").append(boiler).append(", weather=")
        .append(weather).append(", calendar=").append(calendar).append(", sensors=").append(sensors)
        .append(", scheduledExecutor=").append(scheduledExecutor).append(", scanner=").append(scanner)
        .append(", processor=").append(processor).append(", httpAdapter=").append(httpAdapter)
        .append(", desiredTemperature=").append(desiredTemperature).append("]");
    return builder.toString();
  }

  public static void main(String[] args) {
    try {
      Heating heating = new Heating();
      heating.start();
    } catch (HeatingException | IOException e) {
      e.printStackTrace();
    }
  }
}
