package uk.me.steev.java.heating.io.http.set;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
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

      IPAddressString remoteString = new IPAddressString(remoteAddr);
      IPAddress remoteAddress = remoteString.getAddress();

      for (String s : allowedFrom) {
        IPAddressString ipas = new IPAddressString(s);
        IPAddress ipa = ipas.getAddress();
        if ((ipa.isIPv4() && remoteAddress.isIPv4()) ||
            (ipa.isIPv6() && remoteAddress.isIPv6())) {
          IPAddressSeqRange ipasr = ipa.toSequentialRange();
          if (ipasr.contains(remoteAddress)) {
            logger.debug(remoteAddr + " is in range " + s);
            return true;
          }
        }
      }
      logger.debug(remoteAddr + " is not in ranges " + Arrays.toString(allowedFrom));
    } catch (HeatingException e) {
      logger.catching(e);
      e.printStackTrace();
    }
    return false;
  }
}
