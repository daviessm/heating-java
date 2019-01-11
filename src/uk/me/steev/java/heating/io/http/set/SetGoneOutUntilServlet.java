package uk.me.steev.java.heating.io.http.set;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.http.get.CurrentTempServlet;

public class SetGoneOutUntilServlet extends SetServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public SetGoneOutUntilServlet(Heating heating) {
    super(heating);
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (!checkAllowed(request)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    response.setContentType("text/plain");

    LocalDateTime goneOutUntilTime = null;
    String pathInfo = null;
    if (null != request.getPathInfo()) {
      pathInfo = request.getPathInfo().replaceFirst("/", "");
      if (!"null".equals(pathInfo)) {
        goneOutUntilTime = LocalDateTime.parse(pathInfo, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      }

      heating.setGoneOutUntilTime(goneOutUntilTime);
      heating.getProcessor().process();

      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }
  }
}
