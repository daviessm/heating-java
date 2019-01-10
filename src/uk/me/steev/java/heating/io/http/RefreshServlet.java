package uk.me.steev.java.heating.io.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingException;

public class RefreshServlet extends HeatingServlet {
  private static final long serialVersionUID = -4295879852374390014L;
  static final Logger logger = LogManager.getLogger(RefreshServlet.class.getName());

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
        String channelId = request.getHeader("X-Goog-Channel-ID");
        if (heating.getCalendar().getUuid().toString().equals(channelId)) {
          logger.info("Getting latest events for channel " + channelId);
          try {
            heating.getCalendar().update();
            //Run processing now in case a new event should already be in progress
            heating.getProcessor().run();
          } catch (HeatingException he) {
            logger.catching(Level.WARN, he);
          }
        } else {
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
