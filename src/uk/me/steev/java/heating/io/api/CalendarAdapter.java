package uk.me.steev.java.heating.io.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;

public class CalendarAdapter {
  protected HeatingConfiguration config;
  protected Calendar calendar;
  protected List<Event> latestEvents;
  protected EventsUpdater eventsUpdater;

  /** Directory to store user credentials for this application. */
  private static final java.io.File DATA_STORE_DIR = new java.io.File(
      System.getProperty("user.home"), ".credentials/heating-java");

  /** Global instance of the {@link FileDataStoreFactory}. */
  private static FileDataStoreFactory DATA_STORE_FACTORY;

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY =
      JacksonFactory.getDefaultInstance();

  /** Global instance of the HTTP transport. */
  private static HttpTransport HTTP_TRANSPORT;

  /** Global instance of the scopes required by this quickstart.
   *
   * If modifying these scopes, delete your previously saved credentials
   * at ~/.credentials/calendar-java-quickstart
   */
  private static final List<String> SCOPES =
      Arrays.asList(CalendarScopes.CALENDAR_READONLY);

  static {
      try {
          HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
          DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
      } catch (Throwable t) {
          t.printStackTrace();
          System.exit(1);
      }
  }

  public CalendarAdapter(HeatingConfiguration config) throws IOException, HeatingException {
    this.config = config;
    latestEvents = new ArrayList<Event>(10);
    eventsUpdater = new EventsUpdater();
    
    List<String> redirectURLs = new ArrayList<String>();
    redirectURLs.add("urn:ietf:wg:oauth:2.0:oob");
    // Load client secrets.
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
      .setInstalled(new GoogleClientSecrets.Details()
        .setClientId(config.getSetting("calendar", "client_id"))
        .setClientSecret(config.getSetting("calendar", "client_secret"))
        .setTokenUri("https://accounts.google.com/o/oauth2/token")
        .setRedirectUris(redirectURLs)
        .setAuthUri("https://accounts.google.com/o/oauth2/auth"));

      // Build flow and trigger user authorisation request.
      GoogleAuthorizationCodeFlow flow =
              new GoogleAuthorizationCodeFlow.Builder(
                      HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
              .setDataStoreFactory(DATA_STORE_FACTORY)
              .setAccessType("offline")
              .build();
      Credential credential = new AuthorizationCodeInstalledApp(
          flow, new LocalServerReceiver()).authorize("user");
      
      this.calendar = new com.google.api.services.calendar.Calendar.Builder(
          HTTP_TRANSPORT, JSON_FACTORY, credential)
          .setApplicationName("heating-java")
          .build();
  }
  
  public List<Event> getEvents() throws IOException, HeatingException {
    DateTime now = new DateTime(System.currentTimeMillis());
    
    Events events = calendar.events().list(config.getSetting("calendar", "calendar_id"))
        .setMaxResults(10)
        .setTimeMin(now)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute();
    
    return events.getItems();
  }
  
  public HeatingConfiguration getConfig() {
    return config;
  }

  public void setConfig(HeatingConfiguration config) {
    this.config = config;
  }

  public Calendar getCalendar() {
    return calendar;
  }

  public void setCalendar(Calendar calendar) {
    this.calendar = calendar;
  }

  public List<Event> getLatestEvents() {
    return latestEvents;
  }

  public void setLatestEvents(List<Event> latestEvents) {
    this.latestEvents = latestEvents;
  }

  public EventsUpdater getEventsUpdater() {
    return eventsUpdater;
  }

  public void setEventsUpdater(EventsUpdater eventsUpdater) {
    this.eventsUpdater = eventsUpdater;
  }

  public class EventsUpdater implements Runnable {
    public void run() {
      try {
        latestEvents = getEvents();
      } catch (IOException | HeatingException e) {
        //TODO
        e.printStackTrace();
      }
    }
  }
}