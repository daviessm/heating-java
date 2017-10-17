package uk.me.steev.java.heating.io.http;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.AbstractLogger;

import uk.me.steev.java.heating.controller.Heating;

public class HttpAdapter {
  static final Logger logger = LogManager.getLogger(HttpAdapter.class.getName());
  protected static HttpAdapter SINGLETON = null;
  protected static Heating heating = null;
  
  private HttpAdapter(Heating heating) {
    try {
      this.heating = heating;
      org.eclipse.jetty.util.log.Log.setLog(new Jetty2Log4j2Bridge("jetty")); 

      Server server = new Server(8080);
      
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);
      context.addServlet(new ServletHolder(new CurrentTempServlet(heating)), "/current_temp");
      
      server.start();

    } catch (Exception e) {
      logger.catching(e);
    }
  }
  
  public static HttpAdapter getHttpAdapter(Heating heating) {
    if (null == HttpAdapter.SINGLETON)
      HttpAdapter.SINGLETON = new HttpAdapter(heating);
    
    return HttpAdapter.SINGLETON;
  }
  
  /**
   * User: Robert Franz
   * Date: 2015-08-24
   * Time: 20:35
   */
  public class Jetty2Log4j2Bridge extends AbstractLogger
  {
    private org.apache.logging.log4j.Logger logger;
    private String name;

    public Jetty2Log4j2Bridge(String name)
    {
      this.name = name;
      logger = LogManager.getLogger(name);
    }

    @Override
    protected org.eclipse.jetty.util.log.Logger newLogger(String fullname)
    {
      return new Jetty2Log4j2Bridge(fullname);
    }

    public String getName()
    {
      return name;
    }

    public void warn(String msg, Object... args)
    {
      logger.warn(msg, args);
    }

    public void warn(Throwable thrown)
    {
      logger.catching(Level.WARN, thrown);
    }

    public void warn(String msg, Throwable thrown)
    {
      logger.warn(msg, thrown);
    }

    public void info(String msg, Object... args)
    {
      logger.info(msg, args);
    }

    public void info(Throwable thrown)
    {
      logger.catching(Level.INFO, thrown);
    }

    public void info(String msg, Throwable thrown)
    {
      logger.info(msg, thrown);
    }

    public boolean isDebugEnabled()
    {
      return logger.isDebugEnabled();
    }

    public void setDebugEnabled(boolean enabled)
    {
      warn("setDebugEnabled not implemented", null, null);
    }

    public void debug(String msg, Object... args)
    {
      logger.debug(msg, args);
    }

    public void debug(Throwable thrown)
    {
      logger.catching(Level.DEBUG, thrown);
    }

    public void debug(String msg, Throwable thrown)
    {
      logger.debug(msg, thrown);
    }

    public void ignore(Throwable ignored)
    {
      logger.catching(Level.TRACE, ignored);
    }
  }
}
