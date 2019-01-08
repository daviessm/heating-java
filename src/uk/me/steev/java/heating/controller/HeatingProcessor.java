package uk.me.steev.java.heating.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.services.calendar.model.Event;

import uk.me.steev.java.heating.io.boiler.RelayException;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;
import uk.me.steev.java.heating.utils.Processable;

public class HeatingProcessor implements Runnable, Processable {
  static final Logger logger = LogManager.getLogger(HeatingProcessor.class.getName());
  private LocalDateTime timeLastRun;
  private Heating heating;
  private List<TemperatureEvent> timesDueOn;

  public HeatingProcessor(Heating heating) {
    this.heating = heating;
    timeLastRun = LocalDateTime.now();
  }

  public void run() {
    if (!Duration.between(timeLastRun, LocalDateTime.now()).minusMinutes(1).isNegative())
      process();
  }

  public void process() {
    synchronized(heating) {
      LocalDateTime overrideEnd = heating.getOverrideEnd();
      if (overrideEnd.isBefore(LocalDateTime.now())) {
        heating.setOverrideDegrees(0f);
      }
      try {
        int minimumTemperature;
        Duration minutesPerDegree;
        Duration effectDelayMinutes;
        Duration proportionalHeatingIntervalMinutes;
        Duration minimumActivePeriodMinutes;
        double overshootDegrees;
        try {
          minimumTemperature = HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature");
          minutesPerDegree = Duration.ofMinutes(HeatingConfiguration.getIntegerSetting("heating", "minutes_per_degree"));
          effectDelayMinutes = Duration.ofMinutes(HeatingConfiguration.getIntegerSetting("heating", "effect_delay_minutes"));
          proportionalHeatingIntervalMinutes = Duration.ofMinutes(HeatingConfiguration.getIntegerSetting("heating", "proportional_heating_interval_minutes"));
          minimumActivePeriodMinutes = Duration.ofMinutes(HeatingConfiguration.getIntegerSetting("heating", "minimum_active_period_minutes"));
          overshootDegrees = HeatingConfiguration.getDoubleSetting("heating", "overshoot_degrees");
        } catch (HeatingException e) {
          logger.catching(Level.FATAL, e);
          return;
        }

        float desiredTemperature = minimumTemperature;
        float proportion = 0f;

        //Do preheat first, it doesn't rely on temperature
        //Get a list of preheat events
        List<Event> preheatEvents = new ArrayList<>();
        for (Event event : heating.getCalendar().getCachedEvents()) {
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
              if (!heating.getBoiler().isPreheating())
                heating.getBoiler().startPreheating();
            } catch (RelayException re) {
              logger.catching(Level.ERROR, re);
            }
          }
        }
        try {
          if (heating.getBoiler().isPreheating() &&
              !shouldPreheat) {
            logger.info("Preheat should be off");
            heating.getBoiler().stopPreheating();
          }
        } catch (RelayException re) {
          logger.catching(Level.ERROR, re);
        }

        //Now events that are "on"
        List<Event> heatingOnEvents = new ArrayList<>();
        for (Event event : heating.getCalendar().getCachedEvents()) {
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
            proportion = (float) proportionalHeatingIntervalMinutes.getSeconds() / 60;
            logger.info("Heating forced on");
            try {
              if (!heating.getBoiler().isHeating())
                heating.getBoiler().startHeating();
            } catch (RelayException re) {
              logger.catching(Level.ERROR, re);
            }
            break;
          }
        }

        //Now get latest temperatures
        List<Float> allCurrentTemps = new ArrayList<>();
        Float currentTemperature = null;
        for (Entry<String,BluetoothTemperatureSensor> entry : heating.getSensors().entrySet()) {
          BluetoothTemperatureSensor sensor = entry.getValue();
          LocalDateTime lastUpdated = sensor.getTempLastUpdated();
          LocalDateTime lastFailed = sensor.getTempLastFailedUpdate();
          LocalDateTime created = sensor.getCreated();
          if (!(null == lastUpdated) &&
              lastUpdated.isAfter(LocalDateTime.now().minus(3, ChronoUnit.MINUTES))) {
            allCurrentTemps.add(sensor.getCurrentTemperature());
          } else if (null == lastUpdated && null == lastFailed) {
            if (created.isBefore(LocalDateTime.now().minus(3, ChronoUnit.MINUTES))) {
              logger.warn("Sensor  " + sensor.toString() + " was created more than three minutes ago and has never been polled, disconnecting");
              sensor.disconnect();
              sensor.getTemperatureUpdatdaterFuture().cancel(false);
              synchronized (heating.getSensors()) {
                heating.getSensors().remove(entry.getKey());
              }
            } else {
              logger.warn("Sensor time and failed time for " + sensor.toString() + " are null, ignoring (just created?)");
            }
          } else {
            logger.warn("Sensor time for " + sensor.toString() + " is more than three minutes old, disconnecting");
            sensor.disconnect();
            sensor.getTemperatureUpdatdaterFuture().cancel(false);
            synchronized (heating.getSensors()) {
              heating.getSensors().remove(entry.getKey());
            }
          }
        }
        allCurrentTemps.sort(null);

        logger.info("Current temperatures: " + allCurrentTemps.toString());

        if (allCurrentTemps.size() > 0)
          currentTemperature = allCurrentTemps.get(0);

        if (null == currentTemperature) {
          logger.warn("No current temperature from sensors");
          return;
        }
        logger.debug("Current temperature is " + currentTemperature);

        float overrideDegrees = heating.getOverrideDegrees();
        try {
          if (currentTemperature < minimumTemperature + overrideDegrees) {
            logger.info("Current temperature " +
              currentTemperature +
              " is less than minimum temperature " +
              minimumTemperature +
              " (override by " +
              overrideDegrees + ")");
            if (!(heating.getBoiler().isHeating()))
              heating.getBoiler().startHeating();
            return;
          }
        } catch (RelayException re) {
          logger.catching(Level.ERROR, re);
        }

        List<TemperatureEvent> timesDueOn = new ArrayList<>();
        LocalDateTime goneOutUntilTime = heating.getGoneOutUntilTime();
        if (null != goneOutUntilTime && goneOutUntilTime.isBefore(LocalDateTime.now())) {
          goneOutUntilTime = null;
          heating.setGoneOutUntilTime(null);
        }

        for (Event event : heating.getCalendar().getCachedEvents()) {
          try {
            float eventTemperature = Float.parseFloat(event.getSummary());
            LocalDateTime eventStartTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), ZoneId.systemDefault());
            LocalDateTime eventEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()), ZoneId.systemDefault());
            if (eventStartTime.isBefore(LocalDateTime.now()) &&
                eventEndTime.isAfter(LocalDateTime.now())) {
              //Block out the time from any events during the goneOutUntilTime
              if (null != goneOutUntilTime &&
                  goneOutUntilTime.isBefore(eventEndTime)) {
                timesDueOn.add(new TemperatureEvent(goneOutUntilTime, goneOutUntilTime, eventEndTime, eventTemperature));
              } else if (null != goneOutUntilTime &&
                         goneOutUntilTime.isAfter(eventEndTime)){
                timesDueOn.add(new TemperatureEvent(eventStartTime, eventStartTime, eventEndTime, eventTemperature));
              }
            } else if (eventStartTime.isAfter(LocalDateTime.now())) {
              if (null != goneOutUntilTime &&
                  goneOutUntilTime.isAfter(eventStartTime)) {
                eventStartTime = goneOutUntilTime;
                if (eventStartTime.isAfter(eventEndTime))
                  continue;
              }
              if (eventTemperature > currentTemperature) {
                LocalDateTime newEventStartTime = eventStartTime.minus(effectDelayMinutes)
                    .minusSeconds((long) (minutesPerDegree.toMinutes() * (eventTemperature - currentTemperature) * 60));
                timesDueOn.add(new TemperatureEvent(newEventStartTime, eventStartTime, eventEndTime, eventTemperature));
              } else {
                timesDueOn.add(new TemperatureEvent(eventStartTime, eventStartTime, eventEndTime, eventTemperature));
              }
            }
          } catch (NumberFormatException nfe) {
            continue;
          }
        }

        //Put the elements in order of soonest to latest
        timesDueOn.sort(null);
        this.timesDueOn = timesDueOn;
        logger.debug("Times due on: " + timesDueOn.toString());

        if (!forcedOn) {
          try {
            if (timesDueOn.size() > 0) {
              TemperatureEvent timeDueOn = null;
              //Find if we need to warm up for any future events
              for (TemperatureEvent event : timesDueOn) {
                if (event.getStartTime().isBefore(LocalDateTime.now())) {
                  desiredTemperature = event.temperature;
                  continue;
                }

                if (event.getTimeDueOn().isBefore(LocalDateTime.now()))
                  timeDueOn = event;
              }
              //No events to warm up for, use first event in list
              if (null == timeDueOn)
                timeDueOn = timesDueOn.get(0);

              if (timeDueOn.getStartTime().isAfter(LocalDateTime.now()) &&
                  timeDueOn.getTimeDueOn().isBefore(LocalDateTime.now()) &&
                  timeDueOn.getTemperature() < (double) currentTemperature + overshootDegrees &&
                  timeDueOn.getTimeDueOn().plus(minimumActivePeriodMinutes).isBefore(LocalDateTime.now()) &&
                  heating.getBoiler().isHeating()) {
                logger.info("Warming up, temperature will reach desired point, turn off");
                heating.getBoiler().stopHeating();
              } else if (timeDueOn.getStartTime().isAfter(LocalDateTime.now()) &&
                  timeDueOn.getTimeDueOn().isBefore(LocalDateTime.now()) &&
                  timeDueOn.getTemperature() < (double) currentTemperature + overshootDegrees &&
                  timeDueOn.getTimeDueOn().plus(minimumActivePeriodMinutes).isBefore(LocalDateTime.now()) &&
                  !heating.getBoiler().isHeating()) {
                logger.info("Warming up, will overshoot, stay off");
              } else if (timeDueOn.getTimeDueOn().isBefore(LocalDateTime.now())) {
                if (!timeDueOn.getStartTime().isEqual(timeDueOn.getTimeDueOn())) {
                  logger.debug("Warming up, setting override to 0");
                  overrideDegrees = 0f;
                }
                if (timeDueOn.getTemperature() > (double) currentTemperature - overrideDegrees) {
                  logger.debug("Current temperature " + currentTemperature +
                      " overridden by " + overrideDegrees +
                      " is below desired temperature " + timeDueOn.getTemperature() +
                      " in an event starting at " + timeDueOn.getStartTime() +
                      " warming up from  " + timeDueOn.getTimeDueOn());
                  Duration newProportionalTime = Duration.ofSeconds((long) (
                      (timeDueOn.getTemperature() - currentTemperature - overrideDegrees)
                      * proportionalHeatingIntervalMinutes.toMinutes()
                      * 60));
                  if (timeDueOn.getStartTime().isAfter(LocalDateTime.now()) && timeDueOn.getTimeDueOn().isBefore(LocalDateTime.now())) {
                    logger.debug("Warm-up period, stay on until desired temp period starts");
                    newProportionalTime = proportionalHeatingIntervalMinutes;
                  } else if (newProportionalTime.compareTo(minimumActivePeriodMinutes) < 0) {
                    newProportionalTime = minimumActivePeriodMinutes;
                  } else if (newProportionalTime.compareTo(proportionalHeatingIntervalMinutes) > 0) {
                    newProportionalTime = proportionalHeatingIntervalMinutes;
                  }
                  proportion = (float) newProportionalTime.getSeconds() / 60;

                  if (heating.getBoiler().isHeating()) {
                    LocalDateTime timeHeatingOn = heating.getBoiler().getTimeHeatingOn();
                    logger.debug("Heating is on - came on at " + timeHeatingOn + " and proportion is " + newProportionalTime);
                    if (timeHeatingOn.plus(newProportionalTime).compareTo(LocalDateTime.now()) < 0) {
                      heating.getBoiler().stopHeating();
                      //Reschedule processor for when the proportional interval ends
                      heating.getScheduledExecutor().schedule(this, newProportionalTime.toMillis(), TimeUnit.MILLISECONDS);
                    }
                  } else {
                    LocalDateTime timeHeatingOff = heating.getBoiler().getTimeHeatingOff();
                    logger.debug("Heating is off - went off at " + timeHeatingOff + " and proportion is " + newProportionalTime);
                    if (timeHeatingOff.plus(proportionalHeatingIntervalMinutes.minus(newProportionalTime)).compareTo(LocalDateTime.now()) < 0) {
                      heating.getBoiler().startHeating();
                      //Reschedule processor for when the proportional interval ends
                      heating.getScheduledExecutor().schedule(this, newProportionalTime.toMillis(), TimeUnit.MILLISECONDS);
                    }
                  }
                } else {
                  logger.debug("In an event but warmer than the desired temperature");
                  if (heating.getBoiler().isHeating()) {
                    if (heating.getBoiler().getTimeHeatingOn().plus(minimumActivePeriodMinutes).isAfter(LocalDateTime.now())) {
                        logger.debug("Boiler has not been on for minimum time, wait");
                    } else {
                        logger.debug("Boiler is on, turn off");
                        heating.getBoiler().stopHeating();
                    }
                  }
                }
              } else {
                logger.debug("No current demand for heating");
                if (heating.getBoiler().isHeating()) {
                  if (heating.getBoiler().getTimeHeatingOn().plus(minimumActivePeriodMinutes).isAfter(LocalDateTime.now())) {
                      logger.debug("Boiler has not been on for minimum time, wait");
                  } else {
                      logger.debug("Boiler is on, turn off");
                      heating.getBoiler().stopHeating();
                  }
                }
              }
            } else {
              logger.debug("No events where there would be potential heating demand");
              if (heating.getBoiler().isHeating()) {
                logger.debug("Boiler is on, turn off");
                heating.getBoiler().stopHeating();
              }
            }
          } catch (RelayException re) {
            logger.catching(Level.ERROR, re);
          }
          heating.setDesiredTemperature(desiredTemperature);
          heating.setProportion(proportion);
        }
      }  catch (Throwable t) {
        logger.catching(Level.ERROR, t);
      }
      heating.notifyAll();
    }
    timeLastRun = LocalDateTime.now();
  }

  public List<TemperatureEvent> getTimesDueOn() {
    return timesDueOn;
  }
}
