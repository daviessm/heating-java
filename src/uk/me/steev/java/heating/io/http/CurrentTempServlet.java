package uk.me.steev.java.heating.io.http;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.temperature.BluetoothTemperatureSensor;

public class CurrentTempServlet extends HeatingServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  
  public CurrentTempServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    String pathInfo = request.getPathInfo().replaceFirst("/", "");
    Map<String, BluetoothTemperatureSensor> sensors = heating.getSensors();
    if (sensors.containsKey(pathInfo)) {
      Float temperature = sensors.get(pathInfo).getCurrentTemperature();
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println(temperature);
    }
  }
}
