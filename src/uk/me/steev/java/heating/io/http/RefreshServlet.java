package uk.me.steev.java.heating.io.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingException;

public class RefreshServlet extends HeatingServlet {
  private static final long serialVersionUID = -4295879852374390014L;

  public RefreshServlet(Heating heating) {
    super(heating);
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    String pathInfo = request.getPathInfo().replaceFirst("/", "");
    switch(pathInfo) {
    case "events":
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      if (!("sync".equals(request.getHeader("X-Goog-Resource-State")))) {
        //logger.trace(request.getReader().lines().collect(Collectors.joining("\n")));
        if (heating.getCalendar().getUuid().toString().equals(request.getHeader("X-Goog-Channel-ID"))) {
          logger.info("Getting latest events");
          try {
            heating.getCalendar().update();
            //Run processing now in case a new event should already be in progress
            heating.getProcessor().run();
          } catch (HeatingException he) {
            logger.catching(Level.WARN, he);
          }
        } else {
          String channelId = request.getHeader("X-Goog-Channel-ID");
          String resourceId = request.getHeader("X-Goog-Resource-ID");
          logger.info("Stop watching " + channelId + " " + resourceId);
          heating.getCalendar().stopWatching(channelId, resourceId);
        }
      } else {
        logger.debug("Got sync message");
      }
      break;
    default:
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
