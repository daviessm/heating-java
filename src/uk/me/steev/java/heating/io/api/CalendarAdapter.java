package uk.me.steev.java.heating.io.api;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import com.google.api.services.calendar.Calendar.Events.Watch;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;

import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.utils.Processable;

public class CalendarAdapter {
  static final Logger logger = LogManager.getLogger(CalendarAdapter.class.getName());
  protected HeatingConfiguration config;
  protected Calendar calendar;
  protected List<Event> cachedEvents;
  protected EventsUpdater eventsUpdater;
  protected UUID uuid;
  protected String resourceId;
  protected TemporalAmount updateInterval;
  protected Processable afterEventsUpdatedCallback;

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

  public CalendarAdapter() throws IOException, HeatingException {
    cachedEvents = new ArrayList<Event>(10);
    eventsUpdater = new EventsUpdater();

    try {
      int intervalSeconds = HeatingConfiguration.getIntegerSetting("calendar", "update_calendar_interval_seconds");
      updateInterval = Duration.ofSeconds(intervalSeconds);
    } catch (HeatingException he) {
      logger.catching(Level.FATAL, he);
    }

    List<String> redirectURLs = new ArrayList<String>();
    redirectURLs.add("urn:ietf:wg:oauth:2.0:oob");

    logger.trace("Loading client secrets");
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
      .setInstalled(new GoogleClientSecrets.Details()
        .setClientId(HeatingConfiguration.getStringSetting("calendar", "client_id"))
        .setClientSecret(HeatingConfiguration.getStringSetting("calendar", "client_secret"))
        .setTokenUri("https://accounts.google.com/o/oauth2/token")
        .setRedirectUris(redirectURLs)
        .setAuthUri("https://accounts.google.com/o/oauth2/auth"));

      logger.trace("Build flow");
      GoogleAuthorizationCodeFlow flow =
              new GoogleAuthorizationCodeFlow.Builder(
                      HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
              .setDataStoreFactory(DATA_STORE_FACTORY)
              .setAccessType("offline")
              .build();
      logger.trace("Trigger user authorisation request");
      Credential credential = new AuthorizationCodeInstalledApp(
          flow, new LocalServerReceiver()).authorize("user");

      logger.trace("Make calendar object");
      this.calendar = new com.google.api.services.calendar.Calendar.Builder(
          HTTP_TRANSPORT, JSON_FACTORY, credential)
          .setApplicationName("heating-java")
          .build();
  }

  public CalendarAdapter(Processable afterEventsUpdatedCallback) throws IOException, HeatingException {
    this.afterEventsUpdatedCallback = afterEventsUpdatedCallback;
  }

  public void update() throws IOException, HeatingException {
    DateTime now = new DateTime(System.currentTimeMillis());

    String calendarId = HeatingConfiguration.getStringSetting("calendar", "calendar_id");
    String refreshAddress = HeatingConfiguration.getStringSetting("calendar", "refresh_address");

    if (!(null == this.uuid) &&
        !(null == this.resourceId)) {
      stopWatching(uuid.toString(), resourceId);
    }

    this.uuid = UUID.randomUUID();

    logger.debug("Getting calendar events for calendar " + calendarId);
    cachedEvents = calendar.events().list(calendarId)
        .setMaxResults(10)
        .setTimeMin(now)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute()
        .getItems();
    logger.debug("Got events " + cachedEvents);

    Channel channel = new Channel();
    channel.setId(uuid.toString());
    channel.setExpiration(LocalDateTime.now().plus(updateInterval).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    channel.setAddress(refreshAddress);
    channel.setType("web_hook");
    Watch watch = calendar.events().watch(calendarId, channel);
    Channel responseChannel = watch.execute();
    this.resourceId = responseChannel.getResourceId();

    if (null != afterEventsUpdatedCallback) {
      afterEventsUpdatedCallback.process();
    }
  }

  public void stopWatching(String channelId, String resourceId) {
    Channel channel = new Channel();
    channel.setId(channelId);
    channel.setResourceId(resourceId);

    try {
      calendar.channels().stop(channel).execute();
    } catch (IOException ioe) {
      logger.catching(ioe);
    }
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

  public List<Event> getCachedEvents() {
    return cachedEvents;
  }

  public void setCachedEvents(List<Event> cachedEvents) {
    this.cachedEvents = cachedEvents;
  }

  public EventsUpdater getEventsUpdater() {
    return eventsUpdater;
  }

  public void setEventsUpdater(EventsUpdater eventsUpdater) {
    this.eventsUpdater = eventsUpdater;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public class EventsUpdater implements Runnable {
    public void run() {
      try {
        update();
      } catch (IOException | HeatingException e) {
        logger.catching(Level.WARN, e);
        throw new RuntimeException(e);
      } catch (Throwable t) {
        logger.catching(Level.ERROR, t);
        throw new RuntimeException(t);
      }
    }
  }
}
