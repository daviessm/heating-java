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
    if (null != timesDueOn && timesDueOn.size() > 0) {
      for (TemperatureEvent te : timesDueOn) {
        if (te.getStartTime().isBefore(LocalDateTime.now()))
          continue;
  
        json.put("nextsetpoint", te.getTemperature());
        json.put("nexteventstart", te.getStartTime().toString());
        break;
      }
    }
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(json.toString(2));
  }
}
