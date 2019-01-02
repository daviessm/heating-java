package uk.me.steev.java.heating.io.http;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;

public class SetGoneOutUntilServlet extends HeatingServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public SetGoneOutUntilServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");

    String pathInfo = null;
    if (null != request.getPathInfo()) {
      pathInfo = request.getPathInfo().replaceFirst("/", "");
      LocalDateTime goneOutUntilTime = LocalDateTime.parse(pathInfo, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

      heating.setGoneOutUntilTime(goneOutUntilTime);
      heating.getProcessor().process();

      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }
  }
}
