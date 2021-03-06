package uk.me.steev.java.heating.io.http.set;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.TemperatureEvent;
import uk.me.steev.java.heating.io.http.get.CurrentTempServlet;

public class SetOverrideServlet extends SetServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  static final Logger logger = LogManager.getLogger(CurrentTempServlet.class.getName());
  
  public SetOverrideServlet(Heating heating) {
    super(heating);
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (!allowedUpdate(request)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    response.setContentType("text/plain");

    String pathInfo = null;
    if (null != request.getPathInfo()) {
      pathInfo = request.getPathInfo().replaceFirst("/", "");
      Float overrideDegrees = Float.parseFloat(pathInfo);
      List<TemperatureEvent> timesDueOn = heating.getProcessor().getTimesDueOn();
      if (null != timesDueOn && timesDueOn.size() > 0 &&
          Float.compare(0f, overrideDegrees) != 0) {
        for (TemperatureEvent te : timesDueOn) {
          if (te.getStartTime().isBefore(LocalDateTime.now()) &&
              te.getEndTime().isAfter(LocalDateTime.now())) {
            heating.setOverrideEnd(te.getEndTime());
            break;
          } else if (te.getStartTime().isAfter(LocalDateTime.now())) {
            heating.setOverrideEnd(te.getStartTime());
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
