package uk.me.steev.java.heating.io.http;

import java.io.IOException;

  import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Slf4jLog;

import uk.me.steev.java.heating.controller.Heating;
import uk.me.steev.java.heating.io.http.get.AllDetailsAfterProcessServlet;
import uk.me.steev.java.heating.io.http.get.CurrentTempServlet;
import uk.me.steev.java.heating.io.http.get.DesiredTempServlet;
import uk.me.steev.java.heating.io.http.get.ExternalWeatherServlet;
import uk.me.steev.java.heating.io.http.get.ProportionServlet;
import uk.me.steev.java.heating.io.http.get.StatusServlet;
import uk.me.steev.java.heating.io.http.set.SetGoneOutUntilServlet;
import uk.me.steev.java.heating.io.http.set.SetOverrideServlet;

public class HttpAdapter {
  static final Logger logger = LogManager.getLogger(HttpAdapter.class.getName());
  protected static HttpAdapter SINGLETON = null;
  protected static Heating heating = null;
  
  private HttpAdapter(Heating heating) {
    try {
      HttpAdapter.heating = heating;
      org.eclipse.jetty.util.log.Log.setLog(new Slf4jLog());

      Server server = new Server(8080);
      
      ServletHandler handler = new ServletHandler();
      server.setHandler(handler);

      handler.addServletWithMapping(new ServletHolder(new CurrentTempServlet(heating)), "/get/current_temp/*");
      handler.addServletWithMapping(new ServletHolder(new DesiredTempServlet(heating)), "/get/desired_temp");
      handler.addServletWithMapping(new ServletHolder(new ProportionServlet(heating)), "/get/proportion");
      handler.addServletWithMapping(new ServletHolder(new ExternalWeatherServlet(heating)), "/get/weather/*");
      handler.addServletWithMapping(new ServletHolder(new StatusServlet(heating)), "/get/status/*");
      handler.addServletWithMapping(new ServletHolder(new AllDetailsAfterProcessServlet(heating)), "/get/all_details/*");
      handler.addServletWithMapping(new ServletHolder(new SetOverrideServlet(heating)), "/set/override/*");
      handler.addServletWithMapping(new ServletHolder(new SetGoneOutUntilServlet(heating)), "/set/gone_out_until/*");
      handler.addServletWithMapping(new ServletHolder(new RefreshServlet(heating)), "/refresh/*");

      server.setRequestLog(new AccessLogHandler());
      server.start();

    } catch (Exception e) {
      logger.error(e);
    }
  }

  public static HttpAdapter getHttpAdapter(Heating heating) {
    if (null == HttpAdapter.SINGLETON)
      HttpAdapter.SINGLETON = new HttpAdapter(heating);
    
    return HttpAdapter.SINGLETON;
  }

  public class AccessLogHandler extends AbstractNCSARequestLog {
    private Logger logger = LogManager.getLogger(this.getClass().getName());
    {
      this.setPreferProxiedForAddress(true);
    }

    @Override
    protected boolean isEnabled() {
      return true;
    }

    @Override
    public void write(String requestEntry) throws IOException {
      logger.info(requestEntry);
    }
  }
}
