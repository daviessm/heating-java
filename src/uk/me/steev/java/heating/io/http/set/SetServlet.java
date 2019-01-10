package uk.me.steev.java.heating.io.http.set;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.http.HeatingServlet;

public class SetServlet extends HeatingServlet {

  private static final long serialVersionUID = 2388106612034867042L;

  public SetServlet(Heating heating) {
    super(heating);
  }

}
