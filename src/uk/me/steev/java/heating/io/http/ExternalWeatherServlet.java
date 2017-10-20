package uk.me.steev.java.heating.io.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.api.CallFailedException;

public class ExternalWeatherServlet extends HeatingServlet {
  static final Logger logger = LogManager.getLogger(ExternalWeatherServlet.class.getName());
  private static final long serialVersionUID = -7558722807676613014L;

  public ExternalWeatherServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String pathInfo = request.getPathInfo().replaceFirst("/", "");
    switch(pathInfo) {
    case "temperature":
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/plain");
      try {
        response.getWriter().println(heating.getWeather().getLatestTemperature());
      } catch (CallFailedException cfe) {
        logger.catching(cfe);
      }
      break;
    case "apparent_temperature":
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/plain");
      try {
        response.getWriter().println(heating.getWeather().getApparentTemperature());
      } catch (CallFailedException cfe) {
        logger.catching(cfe);
      }
      break;
    default:
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}