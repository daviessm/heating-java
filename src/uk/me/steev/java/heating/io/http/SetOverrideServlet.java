package uk.me.steev.java.heating.io.http;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.TemperatureEvent;

public class SetOverrideServlet extends HeatingServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public SetOverrideServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");

    String pathInfo = null;
    if (null != request.getPathInfo()) {
      pathInfo = request.getPathInfo().replaceFirst("/", "");
      Float overrideDegrees = Float.parseFloat(pathInfo);
      List<TemperatureEvent> timesDueOn = heating.getProcessor().getTimesDueOn();
      if (null != timesDueOn && timesDueOn.size() > 0 &&
          Float.compare(0f, overrideDegrees) == 0) {
        for (TemperatureEvent te : timesDueOn) {
          if (te.getStartTime().isBefore(LocalDateTime.now()) &&
              te.getEndTime().isAfter(LocalDateTime.now())) {
            heating.setOverrideEnd(te.getEndTime());
            break;
          }
        }
      } else {
          heating.setOverrideEnd(LocalDateTime.now());
      }

      heating.setOverrideDegrees(overrideDegrees);
      heating.getProcessor().process();

      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
  }
}
