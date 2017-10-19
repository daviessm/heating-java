package uk.me.steev.java.heating.io.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.me.steev.java.heating.controller.Heating;

public class DesiredTempServlet extends HeatingServlet {
  private static final long serialVersionUID = -4295879852374390014L;

  public DesiredTempServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(heating.getDesiredTemperature());
  }
}
