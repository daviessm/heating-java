package uk.me.steev.java.heating.controller;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.api.services.calendar.model.Event;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import uk.me.steev.java.heating.io.api.CalendarAdapter;
import uk.me.steev.java.heating.io.api.WeatherAdapter;
import uk.me.steev.java.heating.io.boiler.Boiler;
import uk.me.steev.java.heating.io.boiler.Relay;
import uk.me.steev.java.heating.io.boiler.RelayException;
import uk.me.steev.java.heating.io.boiler.RelayTypes;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class Heating {
  static final Logger logger = LogManager.getLogger(Heating.class.getName());
  protected HeatingConfiguration config;
  protected Boiler boiler;
  protected WeatherAdapter weather;
  protected CalendarAdapter calendar;
  protected Map<String,BluetoothTemperatureSensor> sensors;
  protected ScheduledThreadPoolExecutor scheduledExecutor;
  protected SensorScanner scanner;
  protected HeatingProcessor processor;
  
  public Heating(File configFile) throws HeatingException {
    try {
      //Get configuration
      this.config = HeatingConfiguration.getConfiguration(configFile);
      
      //Set up relays
      Relay heatingRelay = Relay.findRelay(RelayTypes.USB_1, config.getRelay("heating"));
      Relay preheatRelay = Relay.findRelay(RelayTypes.USB_1, config.getRelay("preheat"));
      this.boiler = new Boiler(heatingRelay, preheatRelay);
      
      //Set up weather API
      this.weather = new WeatherAdapter(this.config);
      
      //Set up events API
      this.calendar = new CalendarAdapter(this.config);
      
      //Set up an empty set of temperature sensors
      this.sensors = new ConcurrentHashMap<>();
      
      //Set up a thing to run other things periodically
      this.scheduledExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(10);
      
      this.scanner = new SensorScanner();
      this.processor = new HeatingProcessor();
    } catch (RelayException | IOException e) {
      throw new HeatingException("Error creating Heating object", e);
    }
  }

  public Heating() throws HeatingException {
    this(new File("config.json"));
  }
  
  public void start() {
    //Look for new sensors every minute
    this.scheduledExecutor.scheduleAtFixedRate(this.scanner, 0, 1, TimeUnit.MINUTES);
    
    //Get new events every six hours
    this.scheduledExecutor.scheduleAtFixedRate(calendar.getEventsUpdater(), 0, 6, TimeUnit.HOURS);
    
    //Get new outside temperature every fifteen minutes
    this.scheduledExecutor.scheduleAtFixedRate(weather.getUpdater(), 0, 15, TimeUnit.MINUTES);
    
    //Process the whole lot every minute
    this.scheduledExecutor.scheduleAtFixedRate(this.processor, 0, 1, TimeUnit.MINUTES);
  }

  public HeatingConfiguration getConfig() {
    return config;
  }

  public void setConfig(HeatingConfiguration config) {
    this.config = config;
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
  
  public class SensorScanner implements Runnable {
    public void run() {
      Map<String,BluetoothTemperatureSensor> newDevices = BluetoothTemperatureSensor.scanForSensors(sensors);
      synchronized (sensors) {
        for (Entry<String, BluetoothTemperatureSensor> entry : newDevices.entrySet()) {
          if (!sensors.containsKey(entry.getKey())) {
            Runnable task = newDevices.get(entry.getKey()).getTemperatureUpdater();
            scheduledExecutor.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
            sensors.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }
  
  public class HeatingProcessor implements Runnable {
    public void run() {
      //Do preheat first, it doesn't rely on temperature
      //Get a list of preheat events
      List<Event> preheatEvents = new ArrayList<>();
      for (Event event : calendar.getLatestEvents()) {
        if ("preheat".equals(event.getSummary().toLowerCase()))
          preheatEvents.add(event);
      }
      
      //Check if any of them are now
      boolean shouldPreheat = false;
      for (Event event : preheatEvents) {
        LocalDateTime eventStartTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZoneId.systemDefault());
        LocalDateTime eventEndTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()), ZoneId.systemDefault());
        
        if (LocalDateTime.now().isAfter(eventStartTime) &&
            LocalDateTime.now().isBefore(eventEndTime)) {
          shouldPreheat = true;
          logger.info("Preheat should be on");
          try {
            if (!boiler.isPreheating())
              boiler.startPreheating();
          } catch (RelayException re) {
            logger.catching(Level.ERROR, re);
          }
        }
      }
      try {
        if (boiler.isPreheating() &&
            !shouldPreheat) {
          logger.info("Preheat should be off");
          boiler.stopPreheating();
        }
      } catch (RelayException re) {
        logger.catching(Level.ERROR, re);
      }
      
      //Now events that are "on"
      List<Event> heatingOnEvents = new ArrayList<>();
      for (Event event : calendar.getLatestEvents()) {
        if ("on".equals(event.getSummary().toLowerCase()))
          heatingOnEvents.add(event);
      }
      
      //Check if any of them are now
      boolean forcedOn = false;
      for (Event event : heatingOnEvents) {
        LocalDateTime eventStartTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZoneId.systemDefault());
        LocalDateTime eventEndTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()), ZoneId.systemDefault());
        
        if (LocalDateTime.now().isAfter(eventStartTime) &&
            LocalDateTime.now().isBefore(eventEndTime)) {
          forcedOn = true;
          logger.info("Heating forced on");
          try {
            if (!boiler.isHeating())
              boiler.startHeating();
          } catch (RelayException re) {
            logger.catching(Level.ERROR, re);
          }
          break;
        }
      }

      if (!forcedOn) {
        //Now get latest temperatures
        Float currentTemperature = null;
        for (Entry<String,BluetoothTemperatureSensor> entry : sensors.entrySet()) {
          if ((null == currentTemperature ||
               entry.getValue().getCurrentTemperature() < currentTemperature)) {
            if  (null != entry.getValue().getTempLastUpdated()) {
              if (LocalDateTime.now().isBefore(entry.getValue().getTempLastUpdated().plus(2, ChronoUnit.MINUTES))) {
                currentTemperature = entry.getValue().getCurrentTemperature();
              } else {
                logger.warn("Sensor time for " + entry.getValue().toString() + " is more than two minutes old, disconnecting");
                entry.getValue().disconnect();
                sensors.remove(entry);
              }
            }
          }
        }
        
        if (null == currentTemperature) {
          logger.warn("No current temperature from sensors, cannot work under these conditions");
          return;
        }
        logger.debug("Current temperature is " + currentTemperature);
        
        //Get a list of heating events
        List<Event> temperatureEvents = new ArrayList<>();
        for (Event event : calendar.getLatestEvents()) {
          try {
            Float.parseFloat(event.getSummary());
          } catch (NumberFormatException nfe) {
            continue;
          }
          temperatureEvents.add(event);
        }
        
        List<Event> currentTemperatureEvents = new ArrayList<>();
        for (Event event : temperatureEvents) {
          LocalDateTime eventStartTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZoneId.systemDefault());
          LocalDateTime eventEndTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()), ZoneId.systemDefault());
          
          if (LocalDateTime.now().isAfter(eventStartTime) &&
              LocalDateTime.now().isBefore(eventEndTime)) {
            currentTemperatureEvents.add(event);
          }
        }
        
        List<Event> futureTemperatureEvents = new ArrayList<>();
        for (Event event : temperatureEvents) {
          LocalDateTime eventStartTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZoneId.systemDefault());
          
          if (LocalDateTime.now().isBefore(eventStartTime)) {
            futureTemperatureEvents.add(event);
          }
        }

        //Should we be on?
        boolean heatingShouldBeOn = false;
        for (Event event : currentTemperatureEvents) {
          try {
            float desiredTemperature = Float.parseFloat(event.getSummary());
            if (currentTemperature < desiredTemperature &&
                !boiler.isHeating()) {
              logger.info("Boiler should be on because current temperature " + currentTemperature +
                          " is lower than desired temperature " + desiredTemperature);
              heatingShouldBeOn = true;
              break;
            }
          } catch (NumberFormatException | RelayException e) {
            logger.catching(Level.ERROR, e);
          }
        }
        
        try {
          if (!heatingShouldBeOn &&
              boiler.isHeating()) {
            boiler.stopHeating();
          } else if (heatingShouldBeOn &&
                     !boiler.isHeating()) {
            boiler.startHeating();
          }
        } catch (RelayException re) {
          logger.catching(Level.ERROR, re);
        }
      }
    }
  }

  public static void main(String[] args) {
    try {
      Heating heating = new Heating();
      heating.start();
    } catch (HeatingException he) {
      he.printStackTrace();
    }
  }
}
