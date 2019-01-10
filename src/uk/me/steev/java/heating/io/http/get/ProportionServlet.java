package uk.me.steev.java.heating.io.http.get;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.me.steev.java.heating.controller.Heating;

public class ProportionServlet extends GetServlet {
  private static final long serialVersionUID = -3331359205536728036L;

  public ProportionServlet(Heating heating) {
    super(heating);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(heating.getProportion());
  }
}
