package uk.me.steev.java.heating.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResubmittingScheduledExecutor extends ScheduledThreadPoolExecutor {
  static final Logger logger = LogManager.getLogger(ResubmittingScheduledExecutor.class.getName());

  public ResubmittingScheduledExecutor(int corePoolSize) {
    super(corePoolSize);
  }

  public ResubmittingScheduledExecutor(int corePoolSize, RejectedExecutionHandler handler) {
    super(corePoolSize, handler);
  }

  public ResubmittingScheduledExecutor(int corePoolSize, ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  public ResubmittingScheduledExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    if (t == null
        && r instanceof Future<?>
        && ((Future<?>)r).isDone()) {
      try {
        ((Future<?>) r).get();
      } catch (CancellationException ce) {
        t = null; //ignore, because we explicitly cancelled the task
      } catch (ExecutionException ee) {
        t = ee.getCause();
      } catch (InterruptedException ie) {
        // ignore/reset
        Thread.currentThread().interrupt();
      }
    }
    if (t != null) {
      Throwable cause = t.getCause();
      if (null != cause)
        logger.catching(Level.WARN, cause);
      else
        logger.catching(Level.WARN, t);
      logger.info("Resubmitting " + r.toString());
      this.schedule(r, 1, TimeUnit.MINUTES);
    }
  }
}
