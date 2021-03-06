package uk.me.steev.java.heating.io.http.get;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.controller.TemperatureEvent;
import uk.me.steev.java.heating.io.api.CallFailedException;
import uk.me.steev.java.heating.io.boiler.RelayException;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class AllDetailsServlet extends GetServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());

  public AllDetailsServlet(Heating heating) {
    super(heating);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("application/json");
    JSONObject json = new JSONObject();

    String pathInfo = null;
    if (null != request.getPathInfo())
      pathInfo = request.getPathInfo().replaceFirst("/", "");
    if (!(null != pathInfo && "no_wait".equals(pathInfo))) {
      try {
        synchronized(heating) {
          heating.wait();
        }
        Thread.sleep(2000);
      } catch (InterruptedException ie) {
        logger.catching(ie);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }
    }

    Map<String, BluetoothTemperatureSensor> sensors = heating.getSensors();
    Map<String, Float> temps = new TreeMap<String, Float>();
    for (String s : sensors.keySet()) {
      temps.put(sensors.get(s).getName(), sensors.get(s).getCurrentTemperature());
    }
    json.put("temps", temps);

    try {
      json.put("heating", heating.getBoiler().isHeating());
      json.put("preheat", heating.getBoiler().isPreheating());
    } catch (RelayException re) {
      logger.catching(re);
    }

    json.put("currentsetpoint", heating.getDesiredTemperature());
    json.put("proportion", heating.getProportion());

    List<TemperatureEvent> timesDueOn = heating.getProcessor().getTimesDueOn();
    TemperatureEvent next = null;

    try {
      if (null != timesDueOn && timesDueOn.size() > 0) {
        TemperatureEvent te = timesDueOn.get(0);
        if (te.getStartTime().isAfter(LocalDateTime.now())) {
          //Not in an event currently
          json.put("nextsetpoint", te.getTemperature());
          json.put("nexteventstart", te.getStartTime().toString());
        } else if (timesDueOn.size() >= 2) {
          next = timesDueOn.get(1);
          if (te.getEndTime().isEqual(next.getStartTime()) ||
              te.getEndTime().isAfter(next.getStartTime())) {
            //In an event, next starts immediately after
            json.put("nextsetpoint", next.getTemperature());
            json.put("nexteventstart", next.getStartTime().toString());
          } else {
            //In an event, no event immediately after
            json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
            json.put("nexteventstart", te.getEndTime());
          }
        } else {
          //Only one event and we're in it
          json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
          json.put("nexteventstart", te.getEndTime());
        }
      } else {
        //No events
        json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
      }
    } catch (HeatingException re) {
      logger.catching(re);
    }

    json.put("override", heating.getOverrideDegrees());

    json.put("goneoutuntil", null == heating.getGoneOutUntilTime() ? JSONObject.NULL : heating.getGoneOutUntilTime());

    try {
      json.put("outside_temp", String.format("%.2f", heating.getWeather().getLatestTemperature()));
      json.put("outside_apparent_temp", String.format("%.2f", heating.getWeather().getApparentTemperature()));
    } catch (CallFailedException cfe) {
      logger.catching(cfe);
    }

    json.put("allowed_update", allowedUpdate(request));

    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(json.toString(2));
  }
}
