package uk.me.steev.java.heating.io.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.me.steev.java.heating.controller.Heating;

public class CurrentTempServlet extends HttpServlet {
  private static final long serialVersionUID = -2479268631077208608L;
  private Heating heating;
  
  public CurrentTempServlet(Heating heating) {
    this.heating = heating;
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("100");
  }
}
