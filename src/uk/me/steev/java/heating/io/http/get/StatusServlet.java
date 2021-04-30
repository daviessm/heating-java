package uk.me.steev.java.heating.io.http.get;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.boiler.RelayException;

public class StatusServlet extends GetServlet {
  static final Logger logger = LogManager.getLogger(StatusServlet.class.getName());
  private static final long serialVersionUID = -7558722807676613014L;

  public StatusServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String pathInfo = request.getPathInfo().replaceFirst("/", "");
    switch(pathInfo) {
    case "heating":
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/plain");
      try {
        response.getWriter().println(heating.getBoiler().isHeating() ? 1 : 0);
      } catch (RelayException re) {
        logger.warn(re);
      }
      break;
    case "preheat":
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/plain");
      try {
        response.getWriter().println(heating.getBoiler().isPreheating() ? 1 : 0);
      } catch (RelayException re) {
        logger.warn(re);
      }
      break;
    default:
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
