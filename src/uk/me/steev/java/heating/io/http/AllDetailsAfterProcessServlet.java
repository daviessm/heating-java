package uk.me.steev.java.heating.io.http;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.controller.TemperatureEvent;
import uk.me.steev.java.heating.io.boiler.RelayException;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class AllDetailsAfterProcessServlet extends HeatingServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public AllDetailsAfterProcessServlet(Heating heating) {
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
      synchronized(heating) {
        try {
          heating.wait();
        } catch (InterruptedException ie) {
          logger.catching(ie);
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return;
        }
      }
      try {
        Thread.sleep(5000);
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
    List<TemperatureEvent> timesDueOn = heating.getProcessor().getTimesDueOn();
    TemperatureEvent next = null;

    try {
      if (null != timesDueOn && timesDueOn.size() > 0) {
        for (int i = 0; i < timesDueOn.size(); i++) {
          TemperatureEvent te = timesDueOn.get(i);
          if (te.getEndTime().isBefore(LocalDateTime.now()))
            continue;
  
          if (i + 1 < timesDueOn.size()) {
            next = timesDueOn.get(i + 1);
            if (te.getEndTime().isEqual(next.getStartTime()) ||
                te.getEndTime().isAfter(next.getStartTime())) {
              json.put("nextsetpoint", next.getTemperature());
              json.put("nexteventstart", next.getStartTime().toString());
            } else {
              json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
              json.put("nexteventstart", next.getEndTime());
            }
          } else {
            json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
            json.put("nexteventstart", te.getEndTime());
          }
        }
      } else {
        json.put("nextsetpoint", HeatingConfiguration.getIntegerSetting("heating", "minimum_temperature"));
      }
    } catch (HeatingException re) {
      logger.catching(re);
    }

    json.put("override", heating.getOverrideDegrees());

    json.put("goneoutuntil", null == heating.getGoneOutUntilTime() ? JSONObject.NULL : heating.getGoneOutUntilTime());
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(json.toString(2));
  }
}
