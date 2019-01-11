package uk.me.steev.java.heating.io.http.set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;
import uk.me.steev.java.heating.io.http.HeatingServlet;

public class SetServlet extends HeatingServlet {
  private static final long serialVersionUID = 2388106612034867042L;
  static final Logger logger = LogManager.getLogger(SetServlet.class.getName());

  public SetServlet(Heating heating) {
    super(heating);
  }

  protected boolean checkAllowed(HttpServletRequest request) {
    try {
      String[] allowedFrom = HeatingConfiguration.getArray("http", "allow_update_from");

      String remoteAddr = request.getHeader("X-Forwarded-For");
      if (null == remoteAddr) {
        remoteAddr = request.getRemoteAddr();
      }

      for (String s : allowedFrom) {
        SubnetUtils su = new SubnetUtils(s);
        SubnetInfo si = su.getInfo();

        if (si.isInRange(remoteAddr)) {
          logger.debug(remoteAddr + " is in range " + s);
          return true;
        }
      }
      logger.debug(remoteAddr + " is not in ranges " + allowedFrom.toString());
    } catch (HeatingException e) {
      logger.catching(e);
      e.printStackTrace();
    }
    return false;
  }
}
