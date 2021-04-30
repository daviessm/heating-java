package uk.me.steev.java.heating.io.http;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.controller.HeatingConfiguration;
import uk.me.steev.java.heating.controller.HeatingException;

public class HeatingServlet extends HttpServlet {
  static final Logger logger = LogManager.getLogger(HeatingServlet.class.getName());
  private static final long serialVersionUID = -5803431462534409621L;
  protected Heating heating;
  
  protected HeatingServlet(Heating heating) {
    this.heating = heating;
  }

  protected boolean allowedUpdate(HttpServletRequest request) {
    try {
      String[] allowedFrom = HeatingConfiguration.getArray("http", "allow_update_from");

      String remoteAddr = request.getHeader("X-Forwarded-For");
      if (null == remoteAddr) {
        remoteAddr = request.getRemoteAddr();
      }

      //The address sometimes comes in square brackets; remove them
      IPAddressString remoteString = new IPAddressString(remoteAddr.replaceAll("\\[", "").replaceAll("\\]", ""));
      IPAddress remoteAddress = null;
      try {
        remoteAddress = remoteString.toAddress();
      } catch (AddressStringException | IncompatibleAddressException e) {
        logger.warn("Invalid IP address " + remoteString, e);
        return false;
      }

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
