package uk.me.steev.java.heating.io.http.set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.http.HeatingServlet;

public class SetServlet extends HeatingServlet {
  private static final long serialVersionUID = 2388106612034867042L;
  static final Logger logger = LogManager.getLogger(SetServlet.class.getName());

  public SetServlet(Heating heating) {
    super(heating);
  }
}
