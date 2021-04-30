package uk.me.steev.java.heating.io.http.get;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class CurrentTempServlet extends GetServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public CurrentTempServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    String pathInfo = request.getPathInfo().replaceFirst("/", "");
    Map<String, BluetoothTemperatureSensor> sensors = heating.getSensors();
    if (sensors.containsKey(pathInfo.toUpperCase())) {
      Float temperature = sensors.get(pathInfo.toUpperCase()).getCurrentTemperature();
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println(temperature);
      logger.debug("Request for " + pathInfo + ", sent " + temperature);
    }

    for (BluetoothTemperatureSensor sensor : sensors.values()) {
      if (sensor.getName().equals(pathInfo)) {
        Float temperature = sensor.getCurrentTemperature();
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(temperature);
        logger.debug("Request for " + pathInfo + ", sent " + temperature);
        break;
      }
    }
  }
}
