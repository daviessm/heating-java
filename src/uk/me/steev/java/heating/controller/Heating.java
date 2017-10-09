package uk.me.steev.java.heating.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.api.services.calendar.model.Event;

import uk.me.steev.java.heating.io.api.CalendarAdapter;
import uk.me.steev.java.heating.io.api.CallFailedException;
import uk.me.steev.java.heating.io.api.WeatherAdapter;
import uk.me.steev.java.heating.io.boiler.Boiler;
import uk.me.steev.java.heating.io.boiler.Relay;
import uk.me.steev.java.heating.io.boiler.RelayTypes;

public class Heating {
  HeatingConfiguration config;
  Boiler b;
  
  public Heating(File configFile) throws HeatingException {
    this.config = HeatingConfiguration.getConfiguration(configFile);
    Relay heatingRelay = Relay.findRelay(RelayTypes.USB_1, config.getRelay("heating"));
    Relay preheatRelay = Relay.findRelay(RelayTypes.USB_1, config.getRelay("preheat"));
    Boiler b = new Boiler(heatingRelay, preheatRelay);
    b.startHeating();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
    b.stopHeating();
  }

  public Heating() throws HeatingException {
   this(new File("config.json"));
  }
  
  public void run() {
    try {
      //WeatherAdapter weather = new WeatherAdapter(this.config);
      CalendarAdapter calendar = new CalendarAdapter(this.config);
      List<Event> events = calendar.getEvents();
      System.out.println(events);
      //System.out.println(weather.getApparentTemperature() + " " + weather.getLatestTemperature());
      //System.out.println(calendar.update());
    } catch (HeatingException he) {
      //TODO logging
      he.printStackTrace();
    //} catch (CallFailedException cfe) {
      //TODO logging
      //cfe.printStackTrace();
    } catch (IOException ioe) {
      //TODO logging
      ioe.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      Heating heating = new Heating();
      heating.run();
    } catch (HeatingException he) {
      //TODO logging
      he.printStackTrace();
    }
  }
}
