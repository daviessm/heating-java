package uk.me.steev.java.heating.io.http.get;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.me.steev.java.heating.controller.Heating;

public class DesiredTempServlet extends GetServlet {
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
