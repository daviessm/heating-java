package uk.me.steev.java.heating.io.http;

import javax.servlet.http.HttpServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;

public class HeatingServlet extends HttpServlet {
  static final Logger logger = LogManager.getLogger(HeatingServlet.class.getName());
  private static final long serialVersionUID = -5803431462534409621L;
  protected Heating heating;
  
  protected HeatingServlet(Heating heating) {
    this.heating = heating;
  }
}
